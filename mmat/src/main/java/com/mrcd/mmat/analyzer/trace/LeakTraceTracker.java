package com.mrcd.mmat.analyzer.trace;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.IPathsFromGCRootsComputer;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.PathsFromGCRootsTree;
import org.eclipse.mat.snapshot.model.IArray;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.NamedReference;
import org.eclipse.mat.snapshot.model.PrettyPrinter;
import org.eclipse.mat.snapshot.model.ThreadToLocalReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Integer.MAX_VALUE;

/**
 * see : https://github.com/hehonghui/leakcanary-for-eclipse/blob/master/Leakcanary-lib/src/com/squareup/leakcanary/HeapAnalyzer.java
 */
public class LeakTraceTracker {

    private static final String ANONYMOUS_CLASS_NAME_PATTERN = "^.+\\$\\d+$";

    /**
     * find the reference chain
     * @param snapshot
     * @param leakingRef
     * @param excludedRefs
     * @return
     * @throws SnapshotException
     */
    public LeakTrace findLeakTrace(ISnapshot snapshot,
                                   IObject leakingRef, ExcludedRefs excludedRefs) throws SnapshotException {
        // find shortest path to gc root
        PathsFromGCRootsTree gcRootsTree = shortestPathToGcRoots(snapshot, leakingRef, excludedRefs);
        // False alarm, no strong reference path to GC Roots.
        if (gcRootsTree == null) {
            return null;
        }

        LeakTrace leakTrace = buildLeakTrace(snapshot, gcRootsTree, excludedRefs);
        if ( leakTrace != null && !leakTrace.isEmpty() ) {
            leakTrace.getLeakedElement().retainedHeapSize = leakingRef.getRetainedHeapSize() ;
            leakTrace.getLeakedElement().address = "0x" + Long.toHexString(snapshot.mapIdToAddress(leakingRef.getObjectId()));
        }
        return leakTrace;
    }

    private PathsFromGCRootsTree shortestPathToGcRoots(ISnapshot snapshot, IObject leakingRef,
                                                       ExcludedRefs excludedRefs) throws SnapshotException {
        Map<IClass, Set<String>> fieldExcludeMap =
                buildClassExcludeMap(snapshot, excludedRefs.excludeFieldMap);

        IPathsFromGCRootsComputer pathComputer =
                snapshot.getPathsFromGCRoots(leakingRef.getObjectId(), fieldExcludeMap);
        return shortestValidPath(snapshot, pathComputer, excludedRefs);
    }


    private PathsFromGCRootsTree shortestValidPath(ISnapshot snapshot,
                                                   IPathsFromGCRootsComputer pathComputer, ExcludedRefs excludedRefs) throws SnapshotException {
        final Map<IClass, Set<String>> excludedStaticFields =
                buildClassExcludeMap(snapshot, excludedRefs.excludeStaticFieldMap);

        int[] shortestPath;
        while ((shortestPath = pathComputer.getNextShortestPath()) != null) {
            PathsFromGCRootsTree tree = pathComputer.getTree(Collections.singletonList(shortestPath));
            if (validPath(snapshot, tree, excludedStaticFields, excludedRefs)) {
                return tree;
            }
        }
        // No more strong reference path.
        return null;
    }


    private static Map<IClass, Set<String>> buildClassExcludeMap(ISnapshot snapshot,
                                                                 Map<String, Set<String>> excludeMap) throws SnapshotException {
        Map<IClass, Set<String>> classExcludeMap = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : excludeMap.entrySet()) {
            Collection<IClass> refClasses = snapshot.getClassesByName(entry.getKey(), false);
            if (refClasses != null && refClasses.size() == 1) {
                IClass refClass = refClasses.iterator().next();
                classExcludeMap.put(refClass, entry.getValue());
            }
        }
        return classExcludeMap;
    }

    private boolean validPath(ISnapshot snapshot, PathsFromGCRootsTree tree,
                              Map<IClass, Set<String>> excludedStaticFields, ExcludedRefs excludedRefs)
            throws SnapshotException {
        if (excludedStaticFields.isEmpty() && excludedRefs.excludedThreads.isEmpty()) {
            return true;
        }
        // Note: the first child is the leaking object, the last child is the GC root.
        IObject parent = null;
        while (tree != null) {
            IObject child = snapshot.getObject(tree.getOwnId());
            // Static field reference
            if (child instanceof IClass) {
                IClass childClass = (IClass) child;
                Set<String> childClassExcludedFields = excludedStaticFields.get(childClass);
                if (childClassExcludedFields != null) {
                    NamedReference ref = findChildInParent(parent, child, excludedRefs);
                    if (ref != null && childClassExcludedFields.contains(ref.getName())) {
                        return false;
                    }
                }
            } else if (child.getClazz().doesExtend(Thread.class.getName())) {
                if (excludedRefs.excludedThreads.contains(getThreadName(child))) {
                    return false;
                }
            }
            parent = child;
            int[] branchIds = tree.getObjectIds();
            tree = branchIds.length > 0 ? tree.getBranch(branchIds[0]) : null;
        }
        return true;
    }

    private LeakTrace buildLeakTrace(ISnapshot snapshot, PathsFromGCRootsTree tree,
                                     ExcludedRefs excludedRefs) throws SnapshotException {
        List<LeakTraceElement> elements = new ArrayList<>();
        IObject parent = null;
        while (tree != null) {
            IObject child = snapshot.getObject(tree.getOwnId());
            elements.add(0, buildLeakElement(parent, child, excludedRefs));
            parent = child;
            int[] branchIds = tree.getObjectIds();
            tree = branchIds.length > 0 ? tree.getBranch(branchIds[0]) : null;
        }
        return new LeakTrace(elements);
    }


    private LeakTraceElement buildLeakElement(IObject parent, IObject child,
                                              ExcludedRefs excludedRefs) throws SnapshotException {
        LeakTraceElement.Type type = null;
        String referenceName = null;
        NamedReference childRef = findChildInParent(parent, child, excludedRefs);
        if (childRef != null) {
            referenceName = childRef.getName();
            if (child instanceof IClass) {
                type = LeakTraceElement.Type.STATIC_FIELD;
            } else if (childRef instanceof ThreadToLocalReference) {
                type = LeakTraceElement.Type.LOCAL;
            } else {
                type = LeakTraceElement.Type.INSTANCE_FIELD;
            }
        }

        LeakTraceElement.Holder holder;
        String className;
        String extra = null;
        if (child instanceof IClass) {
            IClass clazz = (IClass) child;
            holder = LeakTraceElement.Holder.CLASS;
            className = clazz.getName();
        } else if (child instanceof IArray) {
            holder = LeakTraceElement.Holder.ARRAY;
            IClass clazz = child.getClazz();
            className = clazz.getName();
        } else {
            IClass clazz = child.getClazz();
            className = clazz.getName();
            if (clazz.doesExtend(Thread.class.getName())) {
                holder = LeakTraceElement.Holder.THREAD;
                String threadName = getThreadName(child);
                extra = "(named '" + threadName + "')";
            } else if (className.matches(ANONYMOUS_CLASS_NAME_PATTERN)) {
                String parentClassName = clazz.getSuperClass().getName();
                if (Object.class.getName().equals(parentClassName)) {
                    holder = LeakTraceElement.Holder.OBJECT;
                    // This is an anonymous class implementing an interface. The API does not give access
                    // to the interfaces implemented by the class. Let's see if it's in the class path and
                    // use that instead.
                    try {
                        Class<?> actualClass = Class.forName(clazz.getName());
                        Class<?> implementedInterface = actualClass.getInterfaces()[0];
                        extra = "(anonymous class implements " + implementedInterface.getName() + ")";
                    } catch (ClassNotFoundException ignored) {
                    }
                } else {
                    holder = LeakTraceElement.Holder.OBJECT;
                    // Makes it easier to figure out which anonymous class we're looking at.
                    extra = "(anonymous class extends " + parentClassName + ")";
                }
            } else {
                holder = LeakTraceElement.Holder.OBJECT;
            }
        }
        return new LeakTraceElement(referenceName, type, holder, className, extra);
    }

    private String getThreadName(IObject thread) throws SnapshotException {
        return PrettyPrinter.objectAsString((IObject) thread.resolveValue("name"), MAX_VALUE);
    }

    private NamedReference findChildInParent(IObject parent, IObject child, ExcludedRefs excludedRefs)
            throws SnapshotException {
        if (parent == null) {
            return null;
        }
        Set<String> excludedFields = excludedRefs.excludeFieldMap.get(child.getClazz());
        for (NamedReference childRef : child.getOutboundReferences()) {
            if (childRef.getObjectId() == parent.getObjectId() && (excludedFields == null
                    || !excludedFields.contains(childRef.getName()))) {
                return childRef;
            }
        }
        return null;
    }
}
