package com.crescendo.apps.spreadsheetfile;

import com.crescendo.execution.action.*;import tools.jackson.databind.ObjectMapper;import org.apache.poi.ss.usermodel.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;

@ActionMapping(appKey="spreadsheet-file", actionKey="read")
public class SpreadsheetFileReadHandler extends SpreadsheetFileSupport {
    public SpreadsheetFileReadHandler(ObjectMapper mapper){super(mapper);}
    @Override
public ActionResult execute(ActionContext c){try{String fmt=cfg(c,"format","csv");byte[] bytes=fromB64(cfg(c,"base64",""));List<Map<String,Object>> rows=switch(fmt.toLowerCase()){case "xlsx","xls"->readWorkbook(bytes,cfg(c,"sheetName",""));case "ods"->readOds(bytes);default->readCsv(new String(bytes,StandardCharsets.UTF_8));};return ActionResult.success(Map.of("rows",rows,"count",rows.size(),"format",fmt));}catch(Exception e){return ActionResult.failure("Spreadsheet read failed: "+e.getMessage());}}
    private List<Map<String,Object>> readCsv(String text){List<String> lines=text.lines().toList();if(lines.isEmpty())return List.of();List<String> head=csvLine(lines.get(0));List<Map<String,Object>> out=new ArrayList<>();for(int i=1;i<lines.size();i++){List<String> vals=csvLine(lines.get(i));Map<String,Object> row=new LinkedHashMap<>();for(int j=0;j<head.size();j++)row.put(head.get(j),j<vals.size()?vals.get(j):"");out.add(row);}return out;}
    private List<Map<String,Object>> readWorkbook(byte[] bytes,String sheetName)throws Exception{try(Workbook wb=WorkbookFactory.create(new ByteArrayInputStream(bytes))){Sheet sh=!sheetName.isBlank()?wb.getSheet(sheetName):wb.getSheetAt(0);if(sh==null)return List.of();DataFormatter f=new DataFormatter();Iterator<Row> it=sh.rowIterator();if(!it.hasNext())return List.of();Row header=it.next();List<String> heads=new ArrayList<>();for(Cell cell:header)heads.add(f.formatCellValue(cell));List<Map<String,Object>> out=new ArrayList<>();while(it.hasNext()){Row r=it.next();Map<String,Object> row=new LinkedHashMap<>();for(int i=0;i<heads.size();i++)row.put(heads.get(i),f.formatCellValue(r.getCell(i)));out.add(row);}return out;}}
}
