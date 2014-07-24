package com.geeksaint.springvcr.player.serialize;

import com.geeksaint.springvcr.player.Cassette;
import com.geeksaint.springvcr.player.CassetteNotReadableException;

import java.io.File;
import java.io.IOException;

public class CassetteIO {

  public static Cassette readFrom(File cassetteFile) {
    Cassette cassette = null;
    try {
      cassette = YamlIO.read(cassetteFile, Cassette.class);
    }catch (IOException e) {
      throw new CassetteNotReadableException(e);
    }
    return cassette;
  }
}
