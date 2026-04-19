package com.example.backupmanager.util;

public final class ByteSizeFormatter {

    private ByteSizeFormatter() {
    }

    public static String format(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }
        int unit = 0;
        double size = bytes;
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        while (size >= 1024 && unit < units.length - 1) {
            size /= 1024;
            unit++;
        }
        if (unit == 0) {
            return String.format("%d %s", (long) size, units[unit]);
        }
        return String.format("%.2f %s", size, units[unit]);
    }
}
