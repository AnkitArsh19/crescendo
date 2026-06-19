package com.crescendo.apps.spreadsheetfile;

import com.crescendo.execution.action.*;import tools.jackson.databind.ObjectMapper;import org.apache.poi.hssf.usermodel.HSSFWorkbook;import org.apache.poi.xssf.usermodel.XSSFWorkbook;import org.apache.poi.ss.usermodel.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;

@ActionMapping(appKey="spreadsheet-file", actionKey="write")
public class SpreadsheetFileWriteHandler extends SpreadsheetFileSupport {
    public SpreadsheetFileWriteHandler(ObjectMapper mapper){super(mapper);}
    @Override
public ActionResult execute(ActionContext c){try{List<Map<String,Object>> rows=rows(c.configuration().get("rows"));String fmt=cfg(c,"format","csv");byte[] bytes=switch(fmt.toLowerCase()){case "xlsx"->writeWorkbook(rows,cfg(c,"sheetName","Sheet1"),true);case "xls"->writeWorkbook(rows,cfg(c,"sheetName","Sheet1"),false);case "ods"->writeOds(rows,cfg(c,"sheetName","Sheet1"));default->writeCsv(rows).getBytes(StandardCharsets.UTF_8);};return ActionResult.success(Map.of("base64",b64(bytes),"bytes",bytes.length,"format",fmt));}catch(Exception e){return ActionResult.failure("Spreadsheet write failed: "+e.getMessage());}}
    private String writeCsv(List<Map<String,Object>> rows){if(rows.isEmpty())return "";List<String> heads=new ArrayList<>(rows.get(0).keySet());StringBuilder sb=new StringBuilder(String.join(",",heads)).append("\n");for(Map<String,Object> r:rows){for(int i=0;i<heads.size();i++){if(i>0)sb.append(',');sb.append(esc(r.get(heads.get(i))));}sb.append("\n");}return sb.toString();}
    private byte[] writeWorkbook(List<Map<String,Object>> rows,String sheetName,boolean xlsx)throws Exception{try(Workbook wb=xlsx?new XSSFWorkbook():new HSSFWorkbook();ByteArrayOutputStream out=new ByteArrayOutputStream()){Sheet sh=wb.createSheet(sheetName);if(!rows.isEmpty()){List<String> heads=new ArrayList<>(rows.get(0).keySet());Row h=sh.createRow(0);for(int i=0;i<heads.size();i++)h.createCell(i).setCellValue(heads.get(i));for(int r=0;r<rows.size();r++){Row row=sh.createRow(r+1);for(int c=0;c<heads.size();c++)row.createCell(c).setCellValue(String.valueOf(rows.get(r).getOrDefault(heads.get(c),"")));}}wb.write(out);return out.toByteArray();}}
}
