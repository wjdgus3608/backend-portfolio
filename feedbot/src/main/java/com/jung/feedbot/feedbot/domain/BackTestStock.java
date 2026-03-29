package com.jung.feedbot.feedbot.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BackTestStock {
    @Id
    String stockName;
    FeedPeriod feedPeriod;
    float yearFeedRate;

}