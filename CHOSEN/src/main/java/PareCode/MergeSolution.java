package PareCode;

import org.antlr.v4.runtime.misc.Interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author ZLL
 */
public class MergeSolution {

    public static void merge(List<Interval> intervals) {
        if (intervals.size() == 0 || intervals == null) {
        }
        Collections.sort(intervals, new Comparator<Interval>() {
            @Override
            public int compare(Interval interval1, Interval interval2) {
                return interval1.a - interval2.a;
            }
        });
        List<Interval> merged = new ArrayList<Interval>();
        for (int i = 0; i < intervals.size(); ++i) {
            int L = intervals.get(i).a, R = intervals.get(i).b;
            if (merged.size() == 0 || merged.get(merged.size() - 1).b < L-1) {
                merged.add(new Interval(L, R));
            } else {
                merged.get(merged.size() - 1).b = Math.max(merged.get(merged.size() - 1).b, R);
            }
        }
        intervals.clear();
        intervals.addAll(merged);
    }


//    public static void main(String args[]) {
//        //输入: [[1,3],[2,6],[8,10],[15,18]]
//        //输出: [[1,6],[8,10],[15,18]]
//        //解释: 区间 [1,3] 和 [2,6] 重叠, 将它们合并为 [1,6].
//        Interval interval1 = new Interval(1, 3);
//        Interval interval2 = new Interval(2, 6);
//        Interval interval3 = new Interval(8, 10);
//        Interval interval4 = new Interval(15, 18);
//        var list = new ArrayList<Interval>();
//        list.add(interval1);
//        list.add(interval2);
//        list.add(interval3);
//        list.add(interval4);
//        var result = MergeSolution.merge(list);
//        result.forEach(x -> {
//            System.out.println(x.a + " " + x.b);
//        });
//    }
}


