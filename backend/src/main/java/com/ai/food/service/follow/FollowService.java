package com.ai.food.service.follow;

import com.ai.food.mapper.UserFollowMapper;
import com.ai.food.mapper.UserMapper;
import com.ai.food.model.SysUser;
import com.ai.food.model.UserFollow;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 关注 / 粉丝关系业务。
 * <p>主实体为 {@link UserFollow}，{@code baseMapper} 由 ServiceImpl 父类注入；
 * 由于需要联表查 {@link SysUser}，{@link UserMapper} 通过构造函数注入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService extends ServiceImpl<UserFollowMapper, UserFollow> {

    private final UserMapper userMapper;

    @Transactional
    public Map<String, Object> toggleFollow(Long followerId, Long followingId) {
        Optional<UserFollow> existing = Optional.ofNullable(
                baseMapper.findByFollowerIdAndFollowingId(followerId, followingId));

        boolean isFollowing;
        if (existing.isPresent()) {
            // 使用 mapper 中显式定义的取消关注方法，与原 JPA 派生删除语义一致
            baseMapper.deleteByFollowerIdAndFollowingId(followerId, followingId);
            isFollowing = false;
            log.info("User {} unfollowed user {}", followerId, followingId);
        } else {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            // ponytail: 显式 insert 语义清晰，避免 ServiceImpl.save 的 select-then-decide
            baseMapper.insert(follow);
            isFollowing = true;
            log.info("User {} followed user {}", followerId, followingId);
        }

        long followerCount = baseMapper.countByFollowingId(followingId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isFollowing", isFollowing);
        result.put("followerCount", followerCount);
        return result;
    }

    public Map<String, Object> getFollowingList(Long userId, int page, int size) {
        List<Long> followingIds = baseMapper.findFollowingIdsByUserId(userId);

        int start = page * size;
        int end = Math.min(start + size, followingIds.size());

        List<Map<String, Object>> items = new ArrayList<>();
        if (start < followingIds.size()) {
            List<Long> pageIds = followingIds.subList(start, end);
            Map<Long, SysUser> userMap = new LinkedHashMap<>();
            for (SysUser user : userMapper.findByIdIn(pageIds)) {
                userMap.put(user.getId(), user);
            }
            for (Long followingId : pageIds) {
                SysUser user = userMap.get(followingId);
                if (user != null) {
                    items.add(buildUserMap(user, true));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("size", size);
        result.put("total", followingIds.size());
        return result;
    }

    public Map<String, Object> getFollowersList(Long userId, int page, int size) {
        List<Long> followerIds = baseMapper.findFollowerIdsByUserId(userId);

        int start = page * size;
        int end = Math.min(start + size, followerIds.size());

        Set<Long> myFollowingIds = new HashSet<>(baseMapper.findFollowingIdsByUserId(userId));

        List<Map<String, Object>> items = new ArrayList<>();
        if (start < followerIds.size()) {
            List<Long> pageIds = followerIds.subList(start, end);
            Map<Long, SysUser> userMap = new LinkedHashMap<>();
            for (SysUser user : userMapper.findByIdIn(pageIds)) {
                userMap.put(user.getId(), user);
            }
            for (Long followerId : pageIds) {
                SysUser user = userMap.get(followerId);
                if (user != null) {
                    boolean isFollowing = myFollowingIds.contains(followerId);
                    items.add(buildUserMap(user, isFollowing));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("size", size);
        result.put("total", followerIds.size());
        return result;
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return baseMapper.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public Map<String, Object> getFollowStats(Long userId) {
        long followingCount = baseMapper.countByFollowerId(userId);
        long followerCount = baseMapper.countByFollowingId(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("followingCount", followingCount);
        result.put("followerCount", followerCount);
        return result;
    }

    public List<Long> getFollowerIds(Long userId) {
        return baseMapper.findFollowerIdsByUserId(userId);
    }

    public List<Long> getFollowingIds(Long userId) {
        return baseMapper.findFollowingIdsByUserId(userId);
    }

    public List<Long> getMutualFriendIds(Long userId) {
        Set<Long> followingIds = new HashSet<>(baseMapper.findFollowingIdsByUserId(userId));
        List<Long> followerIds = baseMapper.findFollowerIdsByUserId(userId);

        // 互关 = 我关注的人 ∩ 关注我的人
        followingIds.retainAll(followerIds);
        return new ArrayList<>(followingIds);
    }

    public boolean isMutualFollow(Long userId1, Long userId2) {
        return baseMapper.isMutualFollow(userId1, userId2);
    }

    public Map<String, Object> getMutualFriendsList(Long userId, int page, int size) {
        List<Long> mutualIds = getMutualFriendIds(userId);

        int start = page * size;
        int end = Math.min(start + size, mutualIds.size());

        List<Map<String, Object>> items = new ArrayList<>();
        if (start < mutualIds.size()) {
            List<Long> pageIds = mutualIds.subList(start, end);
            Map<Long, SysUser> userMap = new LinkedHashMap<>();
            for (SysUser user : userMapper.findByIdIn(pageIds)) {
                userMap.put(user.getId(), user);
            }
            for (Long friendId : pageIds) {
                SysUser user = userMap.get(friendId);
                if (user != null) {
                    items.add(buildUserMap(user, true));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("size", size);
        result.put("total", mutualIds.size());
        return result;
    }

    private Map<String, Object> buildUserMap(SysUser user, boolean isFollowing) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", user.getId());
        map.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
        map.put("avatar", user.getAvatar());
        map.put("isFollowing", isFollowing);
        return map;
    }
}
