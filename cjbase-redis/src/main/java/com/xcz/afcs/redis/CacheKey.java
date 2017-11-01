package com.xcz.afcs.redis;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Created by jingang on 2017/11/1.
 */
@Getter
@Setter
public class CacheKey implements Serializable {

      private String prefix;

      private Long expireTime;
}
