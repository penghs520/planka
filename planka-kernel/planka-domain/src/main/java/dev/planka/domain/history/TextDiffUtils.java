package dev.planka.domain.history;

import dev.planka.domain.history.HistoryArgument.TextDiffArg;
import dev.planka.domain.history.HistoryArgument.TextDiffArg.DiffHunk;
import dev.planka.domain.history.HistoryArgument.TextDiffArg.DiffLine;
import dev.planka.domain.history.HistoryArgument.TextDiffArg.DiffLineType;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本差异计算工具类
 * <p>
 * 使用基于行的 LCS（最长公共子序列）算法计算文本差异。
 */
public final class TextDiffUtils {

    /** 上下文行数（差异块前后保留的未变化行数） */
    private static final int CONTEXT_LINES = 2;

    private TextDiffUtils() {
    }

    /**
     * 计算两个文本之间的差异
     *
     * @param oldText 原文本
     * @param newText 新文本
     * @return 差异参数
     */
    public static TextDiffArg computeDiff(String oldText, String newText) {
        String[] oldLines = splitLines(oldText);
        String[] newLines = splitLines(newText);

        // 计算 LCS 并生成差异
        List<DiffLine> allDiffLines = computeDiffLines(oldLines, newLines);

        // 将连续的差异行分组为 hunks
        List<DiffHunk> hunks = groupIntoHunks(allDiffLines, oldLines.length, newLines.length);

        return new TextDiffArg(hunks);
    }

    /**
     * 将文本按行分割
     */
    private static String[] splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        return text.split("\n", -1);
    }

    /**
     * 使用 LCS 算法计算差异行
     */
    private static List<DiffLine> computeDiffLines(String[] oldLines, String[] newLines) {
        int m = oldLines.length;
        int n = newLines.length;

        // 计算 LCS 矩阵
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (oldLines[i - 1].equals(newLines[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // 回溯生成差异
        List<DiffLine> result = new ArrayList<>();
        int i = m, j = n;

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldLines[i - 1].equals(newLines[j - 1])) {
                result.add(0, new DiffLine(DiffLineType.CONTEXT, oldLines[i - 1]));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                result.add(0, new DiffLine(DiffLineType.ADD, newLines[j - 1]));
                j--;
            } else {
                result.add(0, new DiffLine(DiffLineType.DELETE, oldLines[i - 1]));
                i--;
            }
        }

        return result;
    }

    /**
     * 将差异行分组为 hunks
     * <p>
     * 每个 hunk 包含变化的行及其周围的上下文行
     */
    private static List<DiffHunk> groupIntoHunks(List<DiffLine> allLines, int oldTotalLines, int newTotalLines) {
        List<DiffHunk> hunks = new ArrayList<>();

        // 找出所有变化行的索引
        List<Integer> changeIndices = new ArrayList<>();
        for (int i = 0; i < allLines.size(); i++) {
            if (allLines.get(i).type() != DiffLineType.CONTEXT) {
                changeIndices.add(i);
            }
        }

        if (changeIndices.isEmpty()) {
            // 没有变化
            return hunks;
        }

        // 按照间隔分组
        int hunkStart = Math.max(0, changeIndices.get(0) - CONTEXT_LINES);
        int hunkEnd = Math.min(allLines.size() - 1, changeIndices.get(0) + CONTEXT_LINES);

        for (int k = 1; k < changeIndices.size(); k++) {
            int changeIdx = changeIndices.get(k);
            int changeStart = Math.max(0, changeIdx - CONTEXT_LINES);
            int changeEnd = Math.min(allLines.size() - 1, changeIdx + CONTEXT_LINES);

            if (changeStart <= hunkEnd + 1) {
                // 合并到当前 hunk
                hunkEnd = changeEnd;
            } else {
                // 创建新 hunk
                hunks.add(createHunk(allLines, hunkStart, hunkEnd));
                hunkStart = changeStart;
                hunkEnd = changeEnd;
            }
        }

        // 添加最后一个 hunk
        hunks.add(createHunk(allLines, hunkStart, hunkEnd));

        return hunks;
    }

    /**
     * 创建一个 hunk
     */
    private static DiffHunk createHunk(List<DiffLine> allLines, int start, int end) {
        List<DiffLine> hunkLines = new ArrayList<>();
        int oldStart = 1;
        int newStart = 1;
        int oldCount = 0;
        int newCount = 0;

        // 计算起始行号
        for (int i = 0; i < start; i++) {
            DiffLine line = allLines.get(i);
            switch (line.type()) {
                case CONTEXT -> {
                    oldStart++;
                    newStart++;
                }
                case DELETE -> oldStart++;
                case ADD -> newStart++;
            }
        }

        // 收集 hunk 中的行并统计行数
        for (int i = start; i <= end; i++) {
            DiffLine line = allLines.get(i);
            hunkLines.add(line);
            switch (line.type()) {
                case CONTEXT -> {
                    oldCount++;
                    newCount++;
                }
                case DELETE -> oldCount++;
                case ADD -> newCount++;
            }
        }

        return new DiffHunk(oldStart, oldCount, newStart, newCount, hunkLines);
    }
}
