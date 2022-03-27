package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.repository.UserInfoRepository;
import com.imooc.bilibili.dao.repository.VideoRepository;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.domain.Video;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/25
 **/
@Service
public class ElasticSearchService {
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private UserInfoRepository userInfoRepository;
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public void addVideo(Video video) {
        videoRepository.save(video);
    }

    public void addUserInfo(UserInfo userInfo) {
        userInfoRepository.save(userInfo);
    }

    /**
     * 全文检索：关键词（可模糊查询）高亮 + 分页，检索的类型有用户投稿的视频资源、用户信息等
     * <p>
     * request :获取agent ip
     */
    public List<Map<String, Object>> getContents(String keyword, Integer pageNo, Integer pageSize) throws IOException {
        // es中的索引
        String[] indeices = {"videos", "user-infos"};
        // SearchRequest：es原生的查询类，构建查询请求，并指定es中的索引，
        SearchRequest searchRequest = new SearchRequest(indeices);

        //存储查询请求的配置，相当于是 Configuration
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //设置分页参数
        sourceBuilder.from(pageNo - 1);
        sourceBuilder.size(pageSize);
        // 多条件下的查询请求构建器：指定在哪些字段中进行匹配查询，也就是标明(多个）实体中的属性
        MultiMatchQueryBuilder matchQueryBuilder = QueryBuilders.multiMatchQuery(keyword, "title", "nick", "description");
        sourceBuilder.query(matchQueryBuilder);
        //设置超时
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        searchRequest.source(sourceBuilder);

        //设置要高亮显示的字段，array对应MultiMatchQueryBuilder指定在哪些字段中进行匹配查询，使用数组可以循环操作
        String[] array = {"title", "nick", "description"};
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        for (String key : array) {
            highlightBuilder.fields().add(new HighlightBuilder.Field(key));
        }
        highlightBuilder.requireFieldMatch(false); //如果要多个字段进行高亮，要为 false
        highlightBuilder.preTags("<span style=\"color:red\">");
        highlightBuilder.postTags("</span>");

        sourceBuilder.highlighter(highlightBuilder);

        //执行搜索
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        List<Map<String, Object>> arrayList = new ArrayList<>();

        //在查询结果SearchHit中，高亮关键字，一个查询结果中可能含有多个关键字
        for (SearchHit hit : searchResponse.getHits()) {
            //设置要高亮的字段
            Map<String, HighlightField> highlightFieldMap = hit.getHighlightFields();
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            for (String key : array) { //想要高亮的字段数组
                HighlightField field = highlightFieldMap.get(key);
                if (field != null) {
                    Text[] fragments = field.fragments(); // 一句话算一个Text
                    String str = Arrays.toString(fragments);
                    //去掉中括号
                    str = str.substring(1, str.length() - 1);
                    sourceMap.put(key, str);
                }
            }
            arrayList.add(sourceMap);
        }
        return arrayList;
    }


    public Video getVideos(String keyword) {
        return videoRepository.findByTitleLike(keyword);
    }

    public void deleteAllVideos() {
        videoRepository.deleteAll();
    }
}
