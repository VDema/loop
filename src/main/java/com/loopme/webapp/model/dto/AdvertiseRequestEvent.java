package com.loopme.webapp.model.dto;

import lombok.*;

/**
 * Created by Volodymyr Dema. Will see.
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AdvertiseRequestEvent {
    String country;
    String os;
    Integer limit;
}