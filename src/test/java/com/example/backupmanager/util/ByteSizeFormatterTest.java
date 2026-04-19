package com.example.backupmanager.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ByteSizeFormatterTest {

    @Test
    void formatsBytes() {
        assertThat(ByteSizeFormatter.format(0)).isEqualTo("0 B");
        assertThat(ByteSizeFormatter.format(500)).isEqualTo("500 B");
    }

    @Test
    void formatsKilobytes() {
        assertThat(ByteSizeFormatter.format(1536)).contains("KB");
    }

    @Test
    void treatsNegativeAsZero() {
        assertThat(ByteSizeFormatter.format(-10)).isEqualTo("0 B");
    }
}
