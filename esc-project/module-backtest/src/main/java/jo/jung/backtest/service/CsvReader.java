package jo.jung.backtest.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jo.jung.backtest.repo.StockRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@Slf4j
@RequiredArgsConstructor
@Component
@Transactional
public class CsvReader {

    @Value("${my.kospi-path}")
    private String KOSPI_PATH;
    @Value("${my.kosdaq-path}")
    private String KOSDAQ_PATH;

    private final StockRepo stockRepo;
    @PersistenceContext
    private EntityManager entityManager;


    public void readCsvNSaveData() throws Exception {
        log.info("[종목 CSV 읽기 및 적재 실행]");

        //테이블 초기화
        resetTable();

        try (
                InputStream is = getClass().getClassLoader().getResourceAsStream(KOSPI_PATH);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "MS949"))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 2) {
                    String code = parts[0].trim();
                    String name = parts[1].trim();
                    log.info("단축종목코드: " + code + ", 종목명: " + name);
                    insertStock(code, name);
                }
            }
        } catch (Exception e) {
            throw new Exception("CSV 읽기 실패: ",e);
        }

        try (
                InputStream is = getClass().getClassLoader().getResourceAsStream(KOSDAQ_PATH);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "MS949"))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 2) {
                    String code = parts[0].trim();
                    String name = parts[1].trim();
                    log.info("단축종목코드: " + code + ", 종목명: " + name);
                    insertStock(code, name);
                }
            }
        } catch (Exception e) {
            throw new Exception("CSV 읽기 실패: ",e);
        }

        log.info("[종목 CSV 읽기 및 적재 정상종료]");
    }

    private void resetTable(){
        stockRepo.deleteAll();
    }

    private void insertStock(String code, String name) {
        entityManager.createNativeQuery("""
            INSERT INTO stocks (stock_code, stock_name)
            VALUES (:code, :name)
        """)
                .setParameter("code", code)
                .setParameter("name", name)
                .executeUpdate();
    }
}
