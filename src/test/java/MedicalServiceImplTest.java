import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import ru.netology.patient.entity.BloodPressure;
import ru.netology.patient.entity.HealthInfo;
import ru.netology.patient.entity.PatientInfo;
import ru.netology.patient.repository.PatientInfoRepository;
import ru.netology.patient.service.alert.SendAlertService;
import ru.netology.patient.service.medical.MedicalService;
import ru.netology.patient.service.medical.MedicalServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

public class MedicalServiceImplTest {

    private SendAlertService sendAlertService;
    private MedicalService medicalService;

    @BeforeEach
    public void initBeforeEach() {
        PatientInfoRepository patientInfoRepository = Mockito.mock(PatientInfoRepository.class);
        Mockito.when(patientInfoRepository.getById("null")).thenReturn(null);
        Mockito.when(patientInfoRepository.getById("1"))
            .thenReturn(new PatientInfo("1", "Ivan", "Ivanov", LocalDate.of(1991, 7, 12),
                new HealthInfo(new BigDecimal("36.5"), new BloodPressure(120, 60))));
        Mockito.when(patientInfoRepository.getById("2"))
            .thenReturn(new PatientInfo("2", "Ivan", "Petrov", LocalDate.of(1991, 7, 12),
                new HealthInfo(new BigDecimal("38.5"), new BloodPressure(120, 60))));
        sendAlertService = Mockito.mock(SendAlertService.class);
        medicalService = new MedicalServiceImpl(patientInfoRepository, sendAlertService);
    }

    @Test
    public void testExceptionFromGetPatientInfo() {
        var expected = RuntimeException.class;
        Assertions.assertThrows(expected, () -> medicalService.checkBloodPressure("null", new BloodPressure()));
        Assertions.assertThrows(expected, () -> medicalService.checkTemperature("null", new BigDecimal("36.5")));
    }

    @ParameterizedTest
    @MethodSource("sourceForCheckBloodPressure")
    public void testCheckBloodPressure(String patientId, BloodPressure bloodPressure, int times) {
        medicalService.checkBloodPressure(patientId, bloodPressure);
        Mockito.verify(sendAlertService, Mockito.times(times)).send(Mockito.anyString());
    }

    private static Stream<Arguments> sourceForCheckBloodPressure() {
        return Stream.of(
            Arguments.of("1", new BloodPressure(120, 60), 0),
            Arguments.of("1", new BloodPressure(110, 70), 1)
        );
    }

    @Test
    public void testCheckBloodPressureMessage() {
        String expected = "Warning, patient with id: 1, need help";
        medicalService.checkBloodPressure("1", new BloodPressure(110, 70));
        Mockito.verify(sendAlertService, Mockito.times(1)).send(expected);
    }

    @ParameterizedTest
    @MethodSource("sourceForCheckTemperature")
    public void testCheckTemperature(String patientId, BigDecimal temperature, int times) {
        medicalService.checkTemperature(patientId, temperature);
        Mockito.verify(sendAlertService, Mockito.times(times)).send(Mockito.anyString());
    }

    private static Stream<Arguments> sourceForCheckTemperature() {
        return Stream.of(
            Arguments.of("1", new BigDecimal("36.5"), 0),
            Arguments.of("2", new BigDecimal("36.5"), 1)
        );
    }

    @Test
    public void testCheckTemperatureMessage() {
        String expected = "Warning, patient with id: 2, need help";
        medicalService.checkTemperature("2", new BigDecimal("36.5"));
        Mockito.verify(sendAlertService, Mockito.times(1)).send(expected);
    }
}
