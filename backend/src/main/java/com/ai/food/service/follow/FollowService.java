package com.ai.food.service.follow;

import com.ai.food.model.SysUser;
import com.ai.food.model.UserFollow;
import com.ai.food.repository.UserFollowRepository;
import com.ai.food.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    @Transactional
    public Map<String, Object> toggleFollow(Long followerId, Long followingId) {
        Optional<UserFollow> existing = userFollowRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        
        boolean isFollowing;
        if (existing.isPresent()) {
            userFollowRepository.delete(existing.get());
            isFollowing = false;
            log.info("User {} unfollowed user {}", followerId, followingId);
        } else {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            userFollowRepository.save(follow);
            isFollowing = true;
            log.info("User {} followed user {}", followerId, followingId);
        }

        long followerCount = userFollowRepository.countByFollowingId(followingId);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isFollowing", isFollowing);
        result.put("followerCount", followerCount);
        return result;
    }

    public Map<String, Object> getFollowingList(Long userId, int page, int size) {
        List<Long> followingIds = userFollowRepository.findFollowingIdsByUserId(userId);
        
        int start = page * size;
        int end = Math.min(start + size, followingIds.size());
        
        List<Map<String, Object>> items = new ArrayList<>();
        if (start < followingIds.size()) {
            List<Long> pageIds = followingIds.subList(start, end);
            Map<Long, SysUser> userMap = new LinkedHashMap<>();
            for (SysUser user : userRepository.findByIdIn(pageIds)) {
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
        List<Long> followerIds = userFollowRepository.findFollowerIdsByUserId(userId);
        
        int start = page * size;
        int end = Math.min(start + size, followerIds.size());
        
        Set<Long> myFollowingIds = new HashSet<>(userFollowRepository.findFollowingIdsByUserId(userId));
        
        List<Map<String, Object>> items = new ArrayList<>();
        if (start < followerIds.size()) {
            List<Long> pageIds = followerIds.subList(start, end);
            Map<Long, SysUser> userMap = new LinkedHashMap<>();
            for (SysUser user : userRepository.findByIdIn(pageIds)) {
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
        return userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public Map<String, Object> getFollowStats(Long userId) {
        long followingCount = userFollowRepository.countByFollowerId(userId);
        long followerCount = userFollowRepository.countByFollowingId(userId);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("followingCount", followingCount);
        result.put("followerCount", followerCount);
        return result;
    }

    public List<Long> getFollowerIds(Long userId) {
        return userFollowRepository.findFollowerIdsByUserId(userId);
    }

    public List<Long> getFollowingIds(Long userId) {
        return userFollowRepository.findFollowingIdsByUserId(userId);
    }

    public List<Long> getMutualFriendIds(Long userId) {
        Set<Long> followingIds = new HashSet<>(userFollowRepository.findFollowingIdsByUserId(userId));
        List<Long> followerIds = userFollowRepository.findFollowerIdsByUserId(userId);
        
        // 互关 = 我关注的人 ∩ 关注我的人
        followingIds.retainAll(followerIds);
        return new ArrayList<>(followingIds);
    }

    public boolean isMutualFollow(Long userId1, Long userId2) {
        return userFollowRepository.isMutualFollow(userId1, userId2);
    }

    public Map<String, Object> getMutualFriendsList(Long userId, int page, int size) {
        List<Long> mutualIds = getMutualFriendIds(userId);
        
        int start = page * size;
        int end = Math.min(start + size, mutualIds.size());
        
        List<Map<String, Object>> items = new ArrayList<>();
        if (start < mutualIds.size()) {
            List<Long> pageIds = mutualIds.subList(start, end);
            Map<Long, SysUser> userMap = new LinkedHashMap<>();
            for (SysUser user : userRepository.findByIdIn(pageIds)) {
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
