package com.imooc.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author luf
 * @date 2022/03/02 22:08
 **/
@RestController
public class RESTfulApi {
    private final Map<Integer, Map<String, Object>> dataMap;

    public RESTfulApi() {
        dataMap = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", i);
            data.put("name", "name" + i);
            dataMap.put(i, data);
        }
    }

    @GetMapping("/objects/{id}")
    public Map<String, Object> getData(@PathVariable Integer id) {
        return dataMap.get(id);
    }

    @DeleteMapping("/objects/{id}")
    public String deleteData(@PathVariable Integer id) {
        dataMap.remove(id);
        return "delete success";
    }

    @PostMapping("/objects")
    // data 相当于是 map 中的 value
    public String postData(@RequestBody Map<String, Object> data) {
        Integer[] idArray = dataMap.keySet().toArray(new Integer[0]);
        Arrays.sort(idArray);
        int nextId = idArray[idArray.length - 1] + 1;
        dataMap.put(nextId, data);
        return "post success";
    }

    @PutMapping("/objects")
    public String putData(@RequestBody Map<String, Object> data) {
        Integer id = Integer.valueOf(String.valueOf(data.get("id")));
        Map<String, Object> containedData = dataMap.get(id);
        if (Objects.isNull(containedData)) {
            Integer[] idArray = dataMap.keySet().toArray(new Integer[0]);
            Arrays.sort(idArray);
            int nextId = idArray[idArray.length - 1] + 1;
            dataMap.put(nextId, data);
        } else {
            dataMap.put(id, data);
        }
        return "put success";
    }

}
