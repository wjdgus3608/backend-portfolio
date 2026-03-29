package com.jung.logic.service;

import com.jung.domain.stock.Stock;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ReadStockCodesService {

    public static List<Stock> getStockCodes(){
        List<Stock> stockList = new LinkedList<>();

        String basePath = System.getProperty("user.dir");
        String KOSPI_FILE = "";
        String KOSDAQ_FILE = "";

        String os = System.getProperty("os.name").toLowerCase();

        if(os.contains("win")) {
            KOSPI_FILE = basePath + "\\kospi_code.xlsx";
            KOSDAQ_FILE = basePath + "\\kosdaq_code.xlsx";
        }
        else if(os.contains("linux") || os.contains("unix")){
            KOSPI_FILE = basePath + "/kospi_code.xlsx";
            KOSDAQ_FILE = basePath + "/kosdaq_code.xlsx";
        }

        System.out.println(KOSPI_FILE);
        System.out.println(KOSDAQ_FILE);

        List<Stock> kospiList = readExcelFile(KOSPI_FILE);
        List<Stock> kosdaqList = readExcelFile(KOSDAQ_FILE);

        stockList.addAll(kospiList);
        stockList.addAll(kosdaqList);

        System.out.println("total stockList size : "+stockList.size());

        return stockList;
    }

    public static List<Stock> readExcelFile(String path){
        List<Stock> stockList = new LinkedList<>();

        System.out.println("start readExcelFile path : "+path);

        try {
            FileInputStream file = new FileInputStream(path);
            IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);

            //Create Workbook instance holding reference to .xlsx file
            XSSFWorkbook workbook = new XSSFWorkbook(file);

            //Get first/desired sheet from the workbook
            XSSFSheet sheet = workbook.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            //첫번째 행 스킵
            if(rowIterator.hasNext())
                rowIterator.next();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Iterator<Cell> cellIterator = row.cellIterator();
                int j = 0;
                Stock stock = new Stock();
                boolean isNotStock = false;
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    String data = cell.getStringCellValue();

                    switch (j){
                        case 0:
                            //종목코드 6자리 아니면(주식이 아니면) 필터링
                            if(data.length()!=6){
                                isNotStock = true;
                                break;
                            }
                            stock.setStockShortCode(data);
                            break;
                        case 1:
                            stock.setStockNormalCode(data);
                            break;
                        case 2:
                            stock.setStockName(data);
                            break;
                    }
                    if(isNotStock){
                        isNotStock=false;
                        break;
                    }
//                    System.out.println(data);
                    j++;

                    //코드1, 코드2, 종목명만 가져오기(3열까지)
                    if(j>=3) {
                        stockList.add(stock);
                        break;
                    }
                }
//                System.out.println();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("end readExcelFile stockList size : "+stockList.size());

        return stockList;
    }

}
