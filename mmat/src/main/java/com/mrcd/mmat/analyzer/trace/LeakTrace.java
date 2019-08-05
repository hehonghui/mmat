package com.mrcd.mmat.analyzer.trace;

import com.mrcd.mmat.util.CollectionsUtil;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * A chain of references that constitute the shortest strong reference path from
 * a leaking instance to the GC roots. Fixing the leak usually means breaking
 * one of the references in that chain.
 */
public final class LeakTrace implements Serializable {
    private static final long serialVersionUID = -4388262227887058047L;

    public final List<LeakTraceElement> elements;

    LeakTrace(List<LeakTraceElement> elements) {
        this.elements = unmodifiableList(elements);
    }

    public boolean isEmpty() {
        return CollectionsUtil.isEmpty(this.elements) ;
    }

    /**
     * 获取造成内存泄漏的对象, 在列表的最后一个的位置
     * @return
     */
    public LeakTraceElement getLeakedElement() {
        if ( elements != null && elements.size() > 0 ) {
            return elements.get( elements.size() - 1 ) ;
        }
        return null ;
    }

    /**
     * 获取Dominate对象 (Dominate是泄漏的对象到达GC Root的关键点, 解除这里的依赖即可打断引用链, 即解除内存泄漏问题)
     * @return
     */
    public LeakTraceElement getDominateElement() {
        if ( elements != null && elements.size() > 1 ) {
            return elements.get( elements.size() - 2 ) ;
        }
        return null ;
    }


    @Override
    public String toString() {
        return format(false) ;
    }

    /**
     * 格式化内存泄漏信息
     * @param isHtmlFormat 是否格式化为 html 格式
     * @return
     */
    public String format(boolean isHtmlFormat) {
        final StringBuilder sb = new StringBuilder();
        final LeakTraceElement leakedObj = getLeakedElement() ;
        if ( leakedObj != null ) {
            sb.append("* leaked ==> ");
            sb.append(leakedObj).append("\n");
        }
        // 最多打印15条数的堆栈记录
        final int leakDepth = Math.min(15, elements.size() - 1)  - 1;
        for (int i = leakDepth; i >= 0 ; i--) {
            LeakTraceElement element = elements.get(i);
            sb.append("* ");
            for (int j = 0; j < (leakDepth - i); j++) {
                sb.append("   ") ;
            }

            boolean isDominate = i == 0 ;
            if ( isHtmlFormat ) {
                final String spanClass = isDominate ? "dominate_element" : "leak_element" ;
                sb.append(String.format("<span class=\"%s\">", spanClass));
            }

            if ( isHtmlFormat ) {
                if ( isDominate ) {
                    sb.append(element.format(true));
                } else {
                    if ( element.referenceName != null ) {
                        sb.append("<span class=\"normal_leak_field\">").append(element.referenceName).append("</span>").append(" -> ").append(element.format(false));
                    } else {
                        sb.append(element);
                    }
                }
                sb.append("</span>");
            } else {
                sb.append(element.format(true));
            }

            sb.append("\n");
        }
        return sb.toString();
    }
}