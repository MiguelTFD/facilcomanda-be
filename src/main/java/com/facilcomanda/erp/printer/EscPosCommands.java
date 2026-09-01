package com.facilcomanda.erp.printer;

public final class EscPosCommands {
  private EscPosCommands() {}

  public static final byte[] INIT = {27, 64};
  public static final byte[] FONT_NORMAL = {27, 77, 0};
  public static final byte[] FONT_SMALL = {27, 77, 1};
  public static final byte[] CUT_PAPER = {29, 86, 0};
}