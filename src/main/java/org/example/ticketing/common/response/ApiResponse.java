package org.example.ticketing.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
  private boolean success;
  private T data;
  private String message;

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, "OK");
  }

  public static <T> ApiResponse<T> success() {
    return new ApiResponse<>(true, null ,"OK");
  }

  public static <T> ApiResponse<T> fail(String message) {
    return new ApiResponse<>(false, null, message);
  }
}
