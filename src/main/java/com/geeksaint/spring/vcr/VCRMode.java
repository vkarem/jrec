package com.geeksaint.spring.vcr;

public enum VCRMode {
  RECORD,
  PLAY_RECORD,
  PLAY;

  public boolean recording(){
    return this == RECORD || this == PLAY_RECORD;
  }
  public boolean playing(){
    return this == PLAY || this == PLAY_RECORD;
  }
}