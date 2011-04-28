import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;


public class WordFrequencyUtil {
  private HashMap<String, Integer> wordFreq = new HashMap<String, Integer>();
  private HashMap<String, String> stemmedWordMap = new HashMap<String, String>();
  
  public WordFrequencyUtil(String filename, Configuration conf) {
    String textStr = HdfsFileUtil.ReadFileContent(filename, conf);
    String[] lines = textStr.split("\n");
    for (String line : lines) {
      String[] parts = line.split("\t");
      String stemmedWord = StringUtil.stem(parts[0]);
      Integer freq = Integer.parseInt(StringUtil.clean(parts[1]));
      wordFreq.put(stemmedWord, freq);
      stemmedWordMap.put(stemmedWord, StringUtil.clean(parts[0]));
    }
  }
  
  public Integer getFrequency(String word) {
    String stemmedWord = StringUtil.stem(word);
    Integer value = wordFreq.get(stemmedWord);
    if (value == null) {
      value = 0;
    }
    return value;
  }
  
  public boolean isPresent(String word) {
	  String stemmedWord = StringUtil.stem(word);
	  return wordFreq.containsKey(stemmedWord);
  }
  
  public String getStoredString(String word) {
	  String stemmedWord = StringUtil.stem(word);
	  return stemmedWordMap.get(stemmedWord);
  }
}
