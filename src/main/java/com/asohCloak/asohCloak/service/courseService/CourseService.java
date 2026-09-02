package com.asohCloak.asohCloak.service.courseService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {
    @Cacheable(value = "courses", key = "'all'")
    public List<String> getAllCourses() {
        return  List.of(".NET Core", "ASP.NET Core", "C#", "EF Core");
    }

    @Cacheable(value = "courses", key = "#id")
    public String getCourseById(Long id) {
        return  "Spring Boot";
    }

    @CacheEvict(value = "courses", allEntries = true)
    public List<String> createCourse() {
      return  List.of("Java", "Spring Boot", "Spring Data JPA");
    }
}
