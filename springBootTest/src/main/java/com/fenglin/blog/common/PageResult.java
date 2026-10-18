package com.fenglin.blog.common;

import java.util.List;

public record PageResult<T>(long total, long current, long size, long pages, List<T> records) {
}
