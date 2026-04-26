package com.westminster.smartcampus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.westminster.smartcampus.exception.mapper.GlobalExceptionMapper;
import com.westminster.smartcampus.exception.mapper.InvalidRequestExceptionMapper;
import com.westminster.smartcampus.exception.mapper.JsonMappingExceptionMapper;
import com.westminster.smartcampus.exception.mapper.JsonParseExceptionMapper;
import com.westminster.smartcampus.exception.mapper.JsonProcessingExceptionMapper;
import com.westminster.smartcampus.exception.mapper.LinkedResourceNotFoundExceptionMapper;
import com.westminster.smartcampus.exception.mapper.ResourceNotFoundExceptionMapper;
import com.westminster.smartcampus.exception.mapper.ResourceConflictExceptionMapper;
import com.westminster.smartcampus.exception.mapper.RoomNotEmptyExceptionMapper;
import com.westminster.smartcampus.exception.mapper.SensorUnavailableExceptionMapper;
import com.westminster.smartcampus.exception.mapper.WebApplicationExceptionMapper;
import com.westminster.smartcampus.filter.ApiLoggingFilter;
import com.westminster.smartcampus.resource.DiagnosticsResource;
import com.westminster.smartcampus.resource.DiscoveryResource;
import com.westminster.smartcampus.resource.SensorResource;
import com.westminster.smartcampus.resource.SensorRoomResource;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(
                DiscoveryResource.class,
                DiagnosticsResource.class,
                SensorRoomResource.class,
                SensorResource.class,
                InvalidRequestExceptionMapper.class,
                JsonParseExceptionMapper.class,
                JsonMappingExceptionMapper.class,
                JsonProcessingExceptionMapper.class,
                ResourceConflictExceptionMapper.class,
                RoomNotEmptyExceptionMapper.class,
                LinkedResourceNotFoundExceptionMapper.class,
                SensorUnavailableExceptionMapper.class,
                ResourceNotFoundExceptionMapper.class,
                WebApplicationExceptionMapper.class,
                GlobalExceptionMapper.class,
                ApiLoggingFilter.class));
    }
}
