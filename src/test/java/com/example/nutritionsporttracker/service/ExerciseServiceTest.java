package com.example.nutritionsporttracker.service;

import com.example.nutritionsporttracker.model.ExerciseType;
import com.example.nutritionsporttracker.model.User;
import com.example.nutritionsporttracker.model.WorkoutLog;
import com.example.nutritionsporttracker.repository.UserRepository;
import com.example.nutritionsporttracker.repository.WorkoutLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkoutLogRepository workoutLogRepository;

    private ExerciseService exerciseService;

    @BeforeEach
    void setUp() {
        exerciseService = new ExerciseService(
                userRepository,
                workoutLogRepository
        );
    }

    @Test
    void shouldCalculateCaloriesAndTrimNameWhenExerciseIsLogged() {
        User user = createUser(1L, 60.0);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(workoutLogRepository.save(any(WorkoutLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutLog result = exerciseService.logExercise(
                1L,
                ExerciseType.CARDIO,
                "  Running  ",
                30
        );

        assertSame(user, result.getUser());
        assertEquals(ExerciseType.CARDIO, result.getExerciseType());
        assertEquals("Running", result.getExerciseName());
        assertEquals(30, result.getDurationMinutes());
        assertEquals(252.0, result.getCaloriesBurned(), 0.001);

        verify(workoutLogRepository).save(result);
    }

    @Test
    void shouldUseDefaultWeightAndExerciseNameWhenValuesAreMissing() {
        User user = createUser(2L, null);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));

        when(workoutLogRepository.save(any(WorkoutLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutLog result = exerciseService.logExercise(
                2L,
                ExerciseType.STRENGTH,
                "   ",
                20
        );

        assertEquals("Strength", result.getExerciseName());
        assertEquals(147.0, result.getCaloriesBurned(), 0.001);
        assertEquals(20, result.getDurationMinutes());
    }

    @Test
    void shouldRejectExerciseWhenDurationIsZero() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> exerciseService.logExercise(
                        1L,
                        ExerciseType.CARDIO,
                        "Running",
                        0
                )
        );

        assertEquals(
                "durationMinutes must be >= 1",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(workoutLogRepository);
    }

    @Test
    void shouldRejectExerciseWhenDurationIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> exerciseService.logExercise(
                        1L,
                        ExerciseType.CARDIO,
                        "Running",
                        null
                )
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(workoutLogRepository);
    }

    @Test
    void shouldRejectExerciseWhenUserDoesNotExist() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> exerciseService.logExercise(
                        99L,
                        ExerciseType.OTHER,
                        null,
                        15
                )
        );

        assertEquals("Kullanıcı bulunamadı", exception.getMessage());

        verify(workoutLogRepository, never())
                .save(any(WorkoutLog.class));
    }

    @Test
    void shouldReturnExerciseLogsWhenUserExists() {
        WorkoutLog first = new WorkoutLog();
        WorkoutLog second = new WorkoutLog();
        List<WorkoutLog> expected = List.of(first, second);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(workoutLogRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(expected);

        List<WorkoutLog> result =
                exerciseService.getLogsByUser(1L);

        assertSame(expected, result);

        verify(userRepository).existsById(1L);
        verify(workoutLogRepository)
                .findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void shouldRejectLogListingWhenUserDoesNotExist() {
        when(userRepository.existsById(99L)).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> exerciseService.getLogsByUser(99L)
        );

        assertEquals("Kullanıcı bulunamadı", exception.getMessage());

        verify(workoutLogRepository, never())
                .findByUserIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    void shouldDeleteExerciseWhenRecordBelongsToUser() {
        User owner = createUser(1L, 60.0);
        WorkoutLog workoutLog = new WorkoutLog();
        workoutLog.setId(10L);
        workoutLog.setUser(owner);

        when(workoutLogRepository.findById(10L))
                .thenReturn(Optional.of(workoutLog));

        exerciseService.deleteExercise(10L, 1L);

        verify(workoutLogRepository).delete(workoutLog);
    }

    @Test
    void shouldRejectExerciseDeletionWhenRecordBelongsToAnotherUser() {
        User owner = createUser(2L, 70.0);
        WorkoutLog workoutLog = new WorkoutLog();
        workoutLog.setId(10L);
        workoutLog.setUser(owner);

        when(workoutLogRepository.findById(10L))
                .thenReturn(Optional.of(workoutLog));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> exerciseService.deleteExercise(10L, 1L)
        );

        assertEquals(
                "Bu egzersiz kaydını silme yetkiniz yok",
                exception.getMessage()
        );

        verify(workoutLogRepository, never())
                .delete(any(WorkoutLog.class));
    }

    private User createUser(Long id, Double weight) {
        User user = new User();
        user.setId(id);
        user.setWeight(weight);
        return user;
    }
}
