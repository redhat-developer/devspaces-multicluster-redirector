package com.redhat.openshift.devspaces;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupMappingServiceTest {

    @Test
    void singleGroupKey_userInGroup_matches() {
        List<String> userGroups = Arrays.asList("team-alpha", "team-beta");
        assertTrue(GroupMappingService.matchesAllGroups("team-alpha", userGroups));
    }

    @Test
    void singleGroupKey_userNotInGroup_noMatch() {
        List<String> userGroups = Arrays.asList("team-alpha");
        assertFalse(GroupMappingService.matchesAllGroups("team-beta", userGroups));
    }

    @Test
    void multiGroupKey_userInAllGroups_matches() {
        List<String> userGroups = Arrays.asList("team-alpha", "team-beta");
        assertTrue(GroupMappingService.matchesAllGroups("team-alpha, team-beta", userGroups));
    }

    @Test
    void multiGroupKey_userInSomeGroupsOnly_noMatch() {
        List<String> userGroups = Arrays.asList("team-alpha");
        assertFalse(GroupMappingService.matchesAllGroups("team-alpha, team-beta", userGroups));
    }

    @Test
    void multiGroupKey_userInNoGroups_noMatch() {
        List<String> userGroups = Arrays.asList("contractors");
        assertFalse(GroupMappingService.matchesAllGroups("team-alpha, team-beta", userGroups));
    }

    @Test
    void emptyUserGroups_noMatch() {
        assertFalse(GroupMappingService.matchesAllGroups("team-alpha", Collections.emptyList()));
    }

    @Test
    void multiGroupKey_extraWhitespace_matches() {
        List<String> userGroups = Arrays.asList("team-alpha", "team-beta");
        assertTrue(GroupMappingService.matchesAllGroups("  team-alpha  ,  team-beta  ", userGroups));
    }

    @Test
    void threeGroupKey_userInAllThree_matches() {
        List<String> userGroups = Arrays.asList("team-alpha", "team-beta", "contractors");
        assertTrue(GroupMappingService.matchesAllGroups("team-alpha, team-beta, contractors", userGroups));
    }

    @Test
    void threeGroupKey_userMissingOne_noMatch() {
        List<String> userGroups = Arrays.asList("team-alpha", "team-beta");
        assertFalse(GroupMappingService.matchesAllGroups("team-alpha, team-beta, contractors", userGroups));
    }
}
