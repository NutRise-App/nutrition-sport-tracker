package com.example.nutritionsporttracker.service;

import com.example.nutritionsporttracker.model.User;
import com.example.nutritionsporttracker.model.WaterIntake;
import com.example.nutritionsporttracker.repository.UserRepository;
import com.example.nutritionsporttracker.repository.WaterIntakeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaterIntakeServiceTest {

    @Mock
    private WaterIntakeRepository waterIntakeRepository;

    @Mock
    private UserRepository userRepository;

    private WaterIntakeService waterIntakeService;

    @BeforeEach
    void setUp() {
        waterIntakeService = new WaterIntakeService(
                waterIntakeRepository,
                userRepository
        );
    }

    @Test
    void shouldSaveWaterIntake() {
        WaterIntake intake = createWaterIntake(
                1L,
                500,
                LocalDateTime.of(2026, 8, 3, 10, 0)
        );

        when(waterIntakeRepository.save(intake))
                .thenReturn(intake);

        WaterIntake result =
                waterIntakeService.addWaterIntake(intake);

        assertSame(intake, result);
        verify(waterIntakeRepository).save(intake);
    }

    @Test
    void shouldReturnWaterIntakesForUser() {
        List<WaterIntake> expected = List.of(
                createWaterIntake(
                        1L,
                        250,
                        LocalDateTime.of(2026, 8, 3, 9, 0)
                ),
                createWaterIntake(
                        1L,
                        500,
                        LocalDateTime.of(2026, 8, 3, 13, 0)
                )
        );

        when(waterIntakeRepository.findByUserId(1L))
                .thenReturn(expected);

        List<WaterIntake> result =
                waterIntakeService.getWaterIntakesByUserId(1L);

        assertSame(expected, result);
        verify(waterIntakeRepository).findByUserId(1L);
    }

    @Test
    void shouldCalculateDailyTotalForRequestedDate() {
        LocalDate requestedDate = LocalDate.of(2026, 8, 3);

        List<WaterIntake> intakes = List.of(
                createWaterIntake(
                        1L,
                        250,
                        LocalDateTime.of(2026, 8, 3, 8, 0)
                ),
                createWaterIntake(
                        1L,
                        500,
                        LocalDateTime.of(2026, 8, 3, 12, 0)
                ),
                createWaterIntake(
                        1L,
                        300,
                        LocalDateTime.of(2026, 8, 2, 18, 0)
                ),
                createWaterIntake(
                        1L,
                        null,
                        LocalDateTime.of(2026, 8, 3, 20, 0)
                )
        );

        when(waterIntakeRepository.findByUserId(1L))
                .thenReturn(intakes);

        int result = waterIntakeService.calculateDailyWaterIntake(
                1L,
                requestedDate
        );

        assertEquals(750, result);
        verify(waterIntakeRepository).findByUserId(1L);
    }

    @Test
    void shouldReturnZeroWhenThereIsNoWaterIntakeForDate() {
        when(waterIntakeRepository.findByUserId(1L))
                .thenReturn(List.of(
                        createWaterIntake(
                                1L,
                                500,
                                LocalDateTime.of(2026, 8, 2, 10, 0)
                        )
                ));

        int result = waterIntakeService.calculateDailyWaterIntake(
                1L,
                LocalDate.of(2026, 8, 3)
        );

        assertEquals(0, result);
    }

    @Test
    void shouldDeleteWaterIntakeWhenRecordBelongsToUser() {
        WaterIntake intake = createWaterIntake(
                1L,
                500,
                LocalDateTime.of(2026, 8, 3, 10, 0)
        );
        intake.setId(10L);

        when(waterIntakeRepository.findById(10L))
                .thenReturn(Optional.of(intake));

        waterIntakeService.deleteWaterIntake(10L, 1L);

        verify(waterIntakeRepository).delete(intake);
    }

    @Test
    void shouldRejectDeletionWhenWaterIntakeDoesNotExist() {
        when(waterIntakeRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> waterIntakeService.deleteWaterIntake(99L, 1L)
        );

        assertEquals("Su kaydı bulunamadı", exception.getMessage());

        verify(waterIntakeRepository, never())
                .delete(any(WaterIntake.class));
    }

    @Test
    void shouldRejectDeletionWhenRecordBelongsToAnotherUser() {
        WaterIntake intake = createWaterIntake(
                2L,
                500,
                LocalDateTime.of(2026, 8, 3, 10, 0)
        );
        intake.setId(10L);

        when(waterIntakeRepository.findById(10L))
                .thenReturn(Optional.of(intake));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> waterIntakeService.deleteWaterIntake(10L, 1L)
        );

        assertEquals(
                "Bu su kaydını silme yetkiniz yok",
                exception.getMessage()
        );

        verify(waterIntakeRepository, never())
                .delete(any(WaterIntake.class));
    }

    private WaterIntake createWaterIntake(
            Long userId,
            Integer amount,
            LocalDateTime createdAt
    ) {
        User user = new User();
        user.setId(userId);

        WaterIntake intake = new WaterIntake();
        intake.setUser(user);
        intake.setAmountMl(amount);
        intake.setCreatedAt(createdAt);

        return intake;
    }
}
