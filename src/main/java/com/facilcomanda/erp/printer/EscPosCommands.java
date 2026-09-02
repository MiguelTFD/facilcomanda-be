package com.facilcomanda.erp.printer;

public final class EscPosCommands {
    private EscPosCommands() {}

    public static final byte[] INIT = {27, 64};
    public static final byte[] CUT_PAPER = {29, 86, 0};

    public static final byte[] ALIGN_LEFT = {27, 97, 0};
    public static final byte[] ALIGN_CENTER = {27, 97, 1};
    public static final byte[] ALIGN_RIGHT = {27, 97, 2};

    public static final byte[] BOLD_ON = {27, 69, 1};
    public static final byte[] BOLD_OFF = {27, 69, 0};
}