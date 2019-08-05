package com.mrcd.mmat.analyzer.trace;

import com.mrcd.mmat.util.MemorySizeFormat;

import java.io.Serializable;

import static java.util.Locale.US;

/**
 * Represents one reference in the chain of references that holds a leaking
 * object in memory.
 */
public final class LeakTraceElement implements Serializable {
    private static final long serialVersionUID = -1439235519530611998L;

    public enum Type {
        INSTANCE_FIELD, STATIC_FIELD, LOCAL
    }

    public enum Holder {
        OBJECT, CLASS, THREAD, ARRAY
    }

    /**
     * Null if this is the last element in the leak trace, ie the leaking
     * object (Class field).
     */
    public final String referenceName;

    /**
     * Null if this is the last element in the leak trace, ie the leaking
     * object.
     */
    public final Type type;
    public final Holder holder;
    public final String className;

    /** Additional information, may be null. */
    public final String extra;
    /**
     * retained heap size
     */
    public long retainedHeapSize;
    /**
     * leak element address
     */
    public String address = "";

    LeakTraceElement(String referenceName, Type type, Holder holder, String className, String extra) {
        this.referenceName = referenceName;
        this.type = type;
        this.holder = holder;
        this.className = className;
        this.extra = extra;
    }

    @Override
    public String toString() {
        return format(true);
    }


    String format(boolean showField) {
        StringBuilder sb = new StringBuilder();

        if (type == Type.STATIC_FIELD) {
            sb.append("static ");
        }

        if (holder == Holder.ARRAY || holder == Holder.THREAD) {
            sb.append(holder.name().toLowerCase(US) + " ");
        }

        sb.append(className) ;
        if (referenceName != null ) {
            if ( showField ) {
                sb.insert(0, referenceName);
                sb.insert(referenceName.length(), " -> ");
            }
        } else {
            if ( address != null && address.length() > 1 ) {
                final String instanceInfo = String.format(" (%s) instance (%s)", address, MemorySizeFormat.formatMB(retainedHeapSize));
                sb.append(instanceInfo) ;
            } else {
                sb.append(" instance") ;
            }
        }

        if (extra != null) {
            sb.append(" " + extra) ;
        }
        return sb.toString();
    }
}
