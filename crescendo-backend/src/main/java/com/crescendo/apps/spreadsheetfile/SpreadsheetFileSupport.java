package com.crescendo.apps.spreadsheetfile;

import com.crescendo.execution.action.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tools.jackson.databind.ObjectMapper;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("unchecked")
abstract class SpreadsheetFileSupport implements ActionHandler {
    final ObjectMapper mapper;
    SpreadsheetFileSupport(ObjectMapper mapper){this.mapper=mapper;}
    String cfg(ActionContext c,String k,String f){Object v=c.configuration().get(k);return v==null||String.valueOf(v).isBlank()?f:String.valueOf(v);}
    List<Map<String,Object>> rows(Object v) throws Exception {
        if (v instanceof List<?> l) return (List<Map<String,Object>>) l;
        return (List<Map<String,Object>>) mapper.readValue(String.valueOf(v), List.class);
    }
    String b64(byte[] bytes){return Base64.getEncoder().encodeToString(bytes);}
    byte[] fromB64(String b64){return Base64.getDecoder().decode(b64);}
    String esc(Object v){String s=v==null?"":String.valueOf(v);return s.contains(",")||s.contains("\"")||s.contains("\n")?"\""+s.replace("\"","\"\"")+"\"":s;}
    List<String> csvLine(String line){List<String> out=new ArrayList<>();StringBuilder cur=new StringBuilder();boolean q=false;for(int i=0;i<line.length();i++){char ch=line.charAt(i);if(ch=='\"'){if(q&&i+1<line.length()&&line.charAt(i+1)=='\"'){cur.append('"');i++;}else q=!q;}else if(ch==','&&!q){out.add(cur.toString());cur.setLength(0);}else cur.append(ch);}out.add(cur.toString());return out;}
    ByteArrayInputStream stream(String text){return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));}

    List<Map<String,Object>> readOds(byte[] bytes)throws Exception{
        byte[] contentXml = null;
        try(ZipInputStream zin=new ZipInputStream(new ByteArrayInputStream(bytes))){
            ZipEntry entry;
            while((entry=zin.getNextEntry())!=null){
                if("content.xml".equals(entry.getName())){
                    contentXml=zin.readAllBytes();
                    break;
                }
            }
        }
        if(contentXml==null)return List.of();

        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var doc=factory.newDocumentBuilder().parse(new ByteArrayInputStream(contentXml));
        NodeList tables=doc.getElementsByTagNameNS("urn:oasis:names:tc:opendocument:xmlns:table:1.0","table");
        if(tables.getLength()==0)return List.of();
        Element table=(Element)tables.item(0);
        NodeList rowNodes=table.getElementsByTagNameNS("urn:oasis:names:tc:opendocument:xmlns:table:1.0","table-row");
        List<List<String>> rows=new ArrayList<>();
        for(int i=0;i<rowNodes.getLength();i++){
            Element row=(Element)rowNodes.item(i);
            NodeList cells=row.getElementsByTagNameNS("urn:oasis:names:tc:opendocument:xmlns:table:1.0","table-cell");
            List<String> values=new ArrayList<>();
            for(int c=0;c<cells.getLength();c++){
                Element cell=(Element)cells.item(c);
                int repeat=intAttr(cell,"urn:oasis:names:tc:opendocument:xmlns:table:1.0","number-columns-repeated",1);
                String text=cell.getTextContent()==null?"":cell.getTextContent();
                for(int r=0;r<Math.min(repeat,1000);r++)values.add(text);
            }
            if(values.stream().anyMatch(v->!v.isBlank()))rows.add(values);
        }
        if(rows.isEmpty())return List.of();
        List<String> heads=rows.get(0);
        List<Map<String,Object>> out=new ArrayList<>();
        for(int i=1;i<rows.size();i++){
            List<String> values=rows.get(i);
            Map<String,Object> row=new LinkedHashMap<>();
            for(int c=0;c<heads.size();c++)row.put(heads.get(c),c<values.size()?values.get(c):"");
            out.add(row);
        }
        return out;
    }

    byte[] writeOds(List<Map<String,Object>> rows,String sheetName)throws Exception{
        String content=odsContent(rows,sheetName==null||sheetName.isBlank()?"Sheet1":sheetName);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(out)){
            ZipEntry mime=new ZipEntry("mimetype");
            mime.setMethod(ZipEntry.STORED);
            byte[] mimeBytes="application/vnd.oasis.opendocument.spreadsheet".getBytes(StandardCharsets.UTF_8);
            mime.setSize(mimeBytes.length);
            java.util.zip.CRC32 crc=new java.util.zip.CRC32();
            crc.update(mimeBytes);
            mime.setCrc(crc.getValue());
            zip.putNextEntry(mime);
            zip.write(mimeBytes);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("content.xml"));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("META-INF/manifest.xml"));
            zip.write(odsManifest().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private String odsContent(List<Map<String,Object>> rows,String sheetName){
        List<String> heads=rows.isEmpty()?List.of():new ArrayList<>(rows.get(0).keySet());
        StringBuilder sb=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" office:version=\"1.2\"><office:body><office:spreadsheet>");
        sb.append("<table:table table:name=\"").append(xml(sheetName)).append("\">");
        appendOdsRow(sb,heads);
        for(Map<String,Object> row:rows){
            List<String> vals=new ArrayList<>();
            for(String head:heads)vals.add(String.valueOf(row.getOrDefault(head,"")));
            appendOdsRow(sb,vals);
        }
        sb.append("</table:table></office:spreadsheet></office:body></office:document-content>");
        return sb.toString();
    }

    private void appendOdsRow(StringBuilder sb,List<String> values){
        sb.append("<table:table-row>");
        for(String value:values){
            sb.append("<table:table-cell office:value-type=\"string\"><text:p>")
                    .append(xml(value))
                    .append("</text:p></table:table-cell>");
        }
        sb.append("</table:table-row>");
    }

    private String odsManifest(){
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" manifest:version=\"1.2\"><manifest:file-entry manifest:full-path=\"/\" manifest:media-type=\"application/vnd.oasis.opendocument.spreadsheet\"/><manifest:file-entry manifest:full-path=\"content.xml\" manifest:media-type=\"text/xml\"/></manifest:manifest>";
    }

    private int intAttr(Element element,String namespace,String name,int fallback){
        String value=element.getAttributeNS(namespace,name);
        try{return value==null||value.isBlank()?fallback:Integer.parseInt(value);}catch(Exception e){return fallback;}
    }

    private String xml(String value){
        return value==null?"":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
}
