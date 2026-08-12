//package com.kbait.anchack.user.service;
//
//import com.kbait.anchack.user.domain.User;
//import com.kbait.anchack.user.dto.request.UserUpdateRequest;
//import com.kbait.anchack.user.dto.response.UserResponse;
//import com.kbait.anchack.user.exception.UserNotFoundException;
//import com.kbait.anchack.user.mapper.UserMapper;
//import com.kbait.anchack.user.service.impl.UserServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDate;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.catchThrowable;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceImplTest {
//
//    private static final Long USER_ID = 1L;
//
//    @Mock
//    private UserMapper userMapper;
//
//    private UserService userService;
//
//    @BeforeEach
//    void setUp() {
//        userService = new UserServiceImpl(userMapper);
//    }
//
//    @Test
//    void 요청에_담긴_닉네임만_있으면_닉네임만_변경된다() {
//        User existingUser = createUser("기존닉네임", LocalDate.of(1997, 3, 15));
//        when(userMapper.findById(USER_ID)).thenReturn(existingUser);
//        when(userMapper.updateProfile(existingUser)).thenReturn(1);
//
//        UserUpdateRequest request = new UserUpdateRequest();
//        request.setNickname("새닉네임");
//
//        UserResponse response = userService.updateMyProfile(USER_ID, request);
//
//        assertThat(response.getNickname()).isEqualTo("새닉네임");
//        assertThat(existingUser.getBirthDate()).isEqualTo(LocalDate.of(1997, 3, 15));
//    }
//
//    @Test
//    void 존재하지_않는_사용자를_조회하면_예외가_발생한다() {
//        when(userMapper.findById(USER_ID)).thenReturn(null);
//
//        Throwable thrown = catchThrowable(() -> userService.getMyProfile(USER_ID));
//
//        assertThat(thrown).isInstanceOf(UserNotFoundException.class);
//    }
//
//    private User createUser(String nickname, LocalDate birthDate) {
//        User user = new User();
//        user.setId(USER_ID);
//        user.setNickname(nickname);
//        user.setBirthDate(birthDate);
//        return user;
//    }
//}
