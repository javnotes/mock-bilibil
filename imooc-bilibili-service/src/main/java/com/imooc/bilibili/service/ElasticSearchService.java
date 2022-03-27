package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.repository.UserInfoRepository;
import com.imooc.bilibili.dao.repository.VideoRepository;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.domain.Video;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
     * 多类型的关键词全文检索 + 分页
     */
    public List<Map<String, Object>> getContents(String keyword, Integer pageNo, Integer pageSize) {
        String[] indices = {"videos", "user-infos"};
        //SearchRequest：es原生的查询类，构建查询请求
        SearchRequest searchRequest = new SearchRequest(indices);
        //SearchSourceBuilder：查询时的配置

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //SearchSourceBuilder：配置分页
        sourceBuilder.from(pageNo - 1);
        sourceBuilder.size(pageSize);
        //多条件下的查询请求构建器
        MultiMatchQueryBuilder matchQueryBuilder = QueryBuilders.multiMatchQuery(keyword, "title", "nick", "description");
        sourceBuilder.query(matchQueryBuilder);

        searchRequest.source(sourceBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        // 高亮显示
        String[] array = {"title", "nick", "descpription"};
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        for (String key : array) {
            highlightBuilder.fields().add(new HighlightBuilder.Field(key));
        }
        highlightBuilder.requireFieldMatch(false); //如果要多个字段进行高亮，要为 false



        return null;
    }


    public Video getVideos(String keyword) {
        return videoRepository.findByTitleLike(keyword);
    }

    public void deleteAllVideos() {
        videoRepository.deleteAll();
    }
}
