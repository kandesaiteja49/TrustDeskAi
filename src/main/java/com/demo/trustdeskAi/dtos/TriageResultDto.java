package com.demo.trustdeskAi.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TriageResultDto {
    private String category; // shipping, refund, warranty, billing, account_security, general
    private String priority; // low, medium, high, urgent
    private boolean escalationNeeded;
}
