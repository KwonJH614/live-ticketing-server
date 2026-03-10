package org.example.ticketing.performance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PerformanceTestReport {

  private final StringBuilder report = new StringBuilder();
  private final String testName;
  private final long startTime;

  public PerformanceTestReport(String testName) {
    this.testName = testName;
    this.startTime = System.currentTimeMillis();

    report.append("# ").append(testName).append(" 성능 테스트 결과\n\n");
    report.append("**테스트 시작 시간**: ")
        .append(LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        .append("\n\n");
  }

  public void addMetric(String metricName, Object value) {
    report.append("- **").append(metricName).append("**: ")
        .append(value).append("\n");
  }

  public void addSection(String title) {
    report.append("\n## ").append(title).append("\n\n");
  }

  public void saveReport() {
    long duration = System.currentTimeMillis() - startTime;
    report.append("\n**총 소요 시간**: ")
        .append(duration).append(" ms\n");

    try {
      Path dir = Path.of("test-results");
      Files.createDirectories(dir);

      String filename = "performance-"
          + testName.replaceAll(" ", "-")
          + "-" + System.currentTimeMillis() + ".md";

      Path filePath = dir.resolve(filename);

      Files.writeString(filePath, report.toString(), StandardCharsets.UTF_8);

      System.out.println("✅ 성능 테스트 리포트 저장됨: " + filePath);
    } catch (IOException e) {
      throw new RuntimeException("성능 리포트 저장 실패", e);
    }
  }

  public void printConsole() {
    System.out.println("\n" + report);
  }
}