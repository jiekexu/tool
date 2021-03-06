package com.xjc.tool.string.dfa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * <p>
 * 思路： 创建一个FilterSet，枚举了0~65535的所有char是否是某个敏感词开头的状态
 * <p>
 * 判断是否是 敏感词开头 | | 是 不是 获取头节点 OK--下一个字 然后逐级遍历，DFA算法
 *
 */
public class WordFilter {

    /**
     * 存储首字
     */
    private static final FilterSet set = new FilterSet();

    /**
     * 存储节点
     */
    private static final Map<Integer, WordNode> nodes = new HashMap<>(1024, 1);

    /**
     * 停顿词
     */
    private static final Set<Integer> stopwdSet = new HashSet<>();

    /**
     * 敏感词过滤替换
     */
    private static final char SIGN = '*';

    static {
        try {
            init();
        } catch (Exception e) {
            // 加载失败
        }
    }

    /**
     * 初始化敏感词库
     */
    private static void init() {
        addSensitiveWord(readWordFromFile("\\filter\\wd.txt"));
        addStopWord(readWordFromFile("\\filter\\stopwd.txt"));
    }

    /**
     * 增加敏感词
     *
     * @param path
     * @return
     */
    private static List<String> readWordFromFile(String path) {
        List<String> words;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(WordFilter.class.getClassLoader().getResourceAsStream(path)));
            words = new ArrayList<>(1200);
            for (String buf = ""; (buf = br.readLine()) != null; ) {
                if (buf == null || buf.trim().equals(""))
                    continue;
                words.add(buf);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                //skip
            }
        }
        return words;
    }

    /**
     * 增加停顿词
     *
     * @param words
     */
    private static void addStopWord(final List<String> words) {
        if (!isEmpty(words)) {
            char[] chs;
            for (String curr : words) {
                chs = curr.toCharArray();
                for (char c : chs) {
                    stopwdSet.add(charConvert(c));
                }
            }
        }
    }

    /**
     * 添加DFA节点
     *
     * @param words
     */
    private static void addSensitiveWord(final List<String> words) {
        if (!isEmpty(words)) {
            char[] chs;
            int fchar;
            int lastIndex;
            // 首字母节点
            WordNode fnode;
            for (String curr : words) {
                chs = curr.toCharArray();
                fchar = charConvert(chs[0]);
                // 没有首字定义
                if (!set.contains(fchar)) {
                    set.add(fchar);
                    fnode = new WordNode(fchar, chs.length == 1);
                    nodes.put(fchar, fnode);
                } else {
                    fnode = nodes.get(fchar);
                    if (!fnode.isLast() && chs.length == 1)
                        fnode.setLast(true);
                }
                lastIndex = chs.length - 1;
                for (int i = 1; i < chs.length; i++) {
                    fnode = fnode.addIfNoExist(charConvert(chs[i]), i == lastIndex);
                }
            }
        }
    }

    /**
     * 过滤判断 将敏感词转化为成屏蔽词
     *
     * @param src
     * @return
     */
    public static final String doFilter(final String src) {
        if (set != null && nodes != null) {
            char[] chs = src.toCharArray();
            int length = chs.length;
            // 当前检查的字符
            int currc;
            // 当前检查字符的备份
            int cpcurrc;
            int k;
            WordNode node;
            for (int i = 0; i < length; i++) {
                currc = charConvert(chs[i]);
                if (!set.contains(currc)) {
                    continue;
                }
                node = nodes.get(currc);
                if (node == null)
                    continue;
                boolean couldMark = false;
                int markNum = -1;
                // 单字匹配
                if (node.isLast()) {
                    couldMark = true;
                    markNum = 0;
                }

                k = i;
                // copy
                cpcurrc = currc;
                for (; ++k < length; ) {
                    int temp = charConvert(chs[k]);
                    if (temp == cpcurrc)
                        continue;
                    if (stopwdSet != null && stopwdSet.contains(temp))
                        continue;
                    node = node.querySub(temp);
                    if (node == null)
                        break;
                    if (node.isLast()) {
                        couldMark = true;
                        markNum = k - i;
                    }
                    cpcurrc = temp;
                }
                if (couldMark) {
                    for (k = 0; k <= markNum; k++) {
                        chs[k + i] = SIGN;
                    }
                    i = i + markNum;
                }
            }
            return new String(chs);
        }
        return src;
    }

    /**
     * 是否包含敏感词
     *
     * @param src
     * @return
     */
    public static final boolean isContains(final String src) {
        if (set != null && nodes != null) {
            char[] chs = src.toCharArray();
            int length = chs.length;
            // 当前检查的字符
            int currc;
            // 当前检查字符的备份
            int cpcurrc;
            int k;
            WordNode node;
            for (int i = 0; i < length; i++) {
                currc = charConvert(chs[i]);
                if (!set.contains(currc)) {
                    continue;
                }
                node = nodes.get(currc);
                if (node == null)
                    continue;
                boolean couldMark = false;
                // 单字匹配
                if (node.isLast()) {
                    couldMark = true;
                }
                // 继续匹配，以长的优先
                k = i;
                cpcurrc = currc;
                for (; ++k < length; ) {
                    int temp = charConvert(chs[k]);
                    if (temp == cpcurrc)
                        continue;
                    if (stopwdSet != null && stopwdSet.contains(temp))
                        continue;
                    node = node.querySub(temp);
                    if (node == null)
                        break;
                    if (node.isLast()) {
                        couldMark = true;
                    }
                    cpcurrc = temp;
                }
                if (couldMark) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 大写转化为小写 全角转化为半角
     *
     * @param src
     * @return
     */
    private static int charConvert(char src) {
        int r = BCConvert.qj2bj(src);
        return (r >= 'A' && r <= 'Z') ? r + 32 : r;
    }

    /**
     * 判断一个集合是否为空
     *
     * @param col
     * @return
     */
    public static <T> boolean isEmpty(final Collection<T> col) {
        return col == null || col.isEmpty();
    }

    public static void main(String[] args) {
        String s = "你是逗比吗？ｆｕｃｋ！fUcK,你竟然用法轮功，法@!轮!%%%功";
        System.out.println("解析问题： " + s);
        System.out.println("解析字数 : " + s.length());
        String re;
        long nano = System.nanoTime();
        re = WordFilter.doFilter(s);
        nano = (System.nanoTime() - nano);
        System.out.println("解析时间 : " + nano + "ns");
        System.out.println("解析时间 : " + nano / 1000000 + "ms");
        System.out.println(re);
        System.out.println();

        nano = System.nanoTime();
        System.out.println("是否包含敏感词： " + WordFilter.isContains(s));
        nano = (System.nanoTime() - nano);
        System.out.println("解析时间 : " + nano + "ns");
        System.out.println("解析时间 : " + nano / 1000000 + "ms");
    }

}
