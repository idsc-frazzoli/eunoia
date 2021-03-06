// code by jph
package ch.ethz.idsc.edelweis;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeSet;

import ch.ethz.idsc.tensor.io.HomeDirectory;

public class Session {
  private final UserProperties userProperties;
  // public final EdelweisConfig edelweisConfig;
  private final Map<String, BulkParser> map = new LinkedHashMap<>();
  public final String user;
  public final Properties projects;
  public final Properties history;
  public final Properties ignore;
  private final Properties cutoff;

  public Session(String user) {
    this.user = user;
    userProperties = new UserProperties(user);
    // edelweisConfig = TensorProperties.wrap(new EdelweisConfig()).tryLoad(userProperties.file("config"));
    projects = userProperties.load("projects");
    history = userProperties.load("history");
    cutoff = userProperties.load("cutoff");
    ignore = userProperties.load("ignore");
  }

  public void build() {
    for (String project : new TreeSet<>(projects.stringPropertyNames()))
      map.put(project, new BulkParser(new File(projects.getProperty(project)), project, ignore));
  }

  public String cutoff(String project) {
    return cutoff.containsKey(project) //
        ? cutoff.getProperty(project)
        : "";
  }

  public Collection<BulkParser> bulkParsers() {
    return map.values();
  }

  public BulkParser bulkParser(String string) {
    return map.get(string);
  }

  public List<String> syncTestFail(BulkParser bulkParser) {
    if (bulkParser.nonTest()) {
      BulkParser bulkTest = map.get(bulkParser.name() + "-test");
      if (Objects.nonNull(bulkTest)) {
        TestCoverage testCoverage = new TestCoverage(bulkParser.root(), bulkTest.root());
        return testCoverage.visitMain();
      }
    } else {
      String name = bulkParser.name();
      BulkParser bulkMain = map.get(name.substring(0, name.length() - 5));
      if (Objects.nonNull(bulkMain)) {
        TestCoverage testCoverage = new TestCoverage(bulkMain.root(), bulkParser.root());
        return testCoverage.visitTest();
      }
    }
    return Collections.emptyList();
  }

  public void showStats() {
    for (Entry<String, BulkParser> entry : map.entrySet()) {
      System.out.println(entry.getKey());
      BulkParser bulkParser = entry.getValue();
      System.out.println(" test=" + bulkParser.nonTest());
      System.out.println(" skip=" + bulkParser.unknownExtensions());
    }
  }

  public File exportFolder() {
    File file = HomeDirectory.Documents("edelweis", user);
    file.mkdirs();
    return file;
  }
}
