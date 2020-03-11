package org.iota.compass.conf;

import com.beust.jcommander.IStringConverter;
import org.iota.jota.pow.SpongeFactory;

public class SpongeModeConverter implements IStringConverter<SpongeFactory.Mode> {
  @Override
  public SpongeFactory.Mode convert(String s) {
    System.out.println(s);
    if(s.startsWith("CURL")){
      s = s.replaceAll("CURL", "CURL_");
    }
    return SpongeFactory.Mode.valueOf(s);
  }
}
