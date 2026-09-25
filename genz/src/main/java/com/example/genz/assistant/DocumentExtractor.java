package com.example.genz.assistant;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.stream.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentExtractor {
    public record Chunk(String content, Integer pageNumber) {}
    public record Extracted(String name, String type, List<Chunk> chunks) {}
    static final int MAX_TEXT = 200_000;
    public Extracted extract(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
            throw new IllegalArgumentException("Upload a nonempty file of at most 10 MB");
        String name = Objects.toString(file.getOriginalFilename(), "document").replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty() || name.length() > 255) throw new IllegalArgumentException("Invalid document name");
        String type = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        var chunks = new ArrayList<Chunk>();
        try {
            switch (type) {
                case "txt", "md" -> add(chunks, StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(file.getBytes())).toString(), null);
                case "pdf" -> {
                    try (var pdf = Loader.loadPDF(file.getBytes())) {
                        if (pdf.isEncrypted() || pdf.getNumberOfPages() > 100)
                            throw new IllegalArgumentException("Use an unencrypted PDF of at most 100 pages");
                        var stripper = new PDFTextStripper();
                        int total = 0;
                        for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                            stripper.setStartPage(page); stripper.setEndPage(page);
                            String text = stripper.getText(pdf);
                            total += text.length();
                            if (total > MAX_TEXT) throw new IllegalArgumentException("Document text is too large");
                            add(chunks, text, page);
                        }
                    }
                }
                case "docx" -> add(chunks, docx(file.getBytes()), null);
                default -> throw new IllegalArgumentException("Supported formats: PDF, DOCX, TXT, MD");
            }
        } catch (IOException | XMLStreamException ex) {
            throw new IllegalArgumentException("Document is unreadable or invalid");
        }
        if (chunks.isEmpty()) throw new IllegalArgumentException("No readable text found; scanned PDFs require OCR");
        if (chunks.size() > 100) throw new IllegalArgumentException("Document exceeds 100 chunks");
        return new Extracted(name, type, List.copyOf(chunks));
    }
    static void add(List<Chunk> chunks, String text, Integer page) {
        if (text.length() > MAX_TEXT) throw new IllegalArgumentException("Document text is too large");
        text = text.replace("\u0000", "").replaceAll("\\s+", " ").trim();
        for (int start = 0; start < text.length();) {
            int end = Math.min(start + 2400, text.length());
            if (end < text.length()) {
                if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
                int boundary = text.lastIndexOf(' ', end);
                if (boundary > start + 1800) end = boundary;
            }
            chunks.add(new Chunk(text.substring(start, end), page));
            if (end == text.length()) break;
            start = end - 200;
            if (Character.isLowSurrogate(text.charAt(start))) start++;
        }
    }
    private static String docx(byte[] bytes) throws IOException, XMLStreamException {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            int entries = 0;
            long expanded = 0;
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                if (++entries > 1000) throw new IllegalArgumentException("DOCX has too many entries");
                byte[] data = zip.readNBytes(2_000_001);
                expanded += data.length;
                if (data.length > 2_000_000 || expanded > 20_000_000)
                    throw new IllegalArgumentException("DOCX expanded content is too large");
                if (!"word/document.xml".equals(entry.getName())) continue;
                var factory = XMLInputFactory.newFactory();
                factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
                factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
                var reader = factory.createXMLStreamReader(new ByteArrayInputStream(data));
                var text = new StringBuilder();
                try {
                    while (reader.hasNext()) {
                        int event = reader.next();
                        if (event == XMLStreamConstants.DTD) throw new IllegalArgumentException("DTD is not supported");
                        if (event == XMLStreamConstants.START_ELEMENT && "t".equals(reader.getLocalName()))
                            text.append(reader.getElementText());
                        if (event == XMLStreamConstants.END_ELEMENT && Set.of("p", "tc").contains(reader.getLocalName()))
                            text.append('\n');
                        if (text.length() > MAX_TEXT) throw new IllegalArgumentException("Document text is too large");
                    }
                } finally { reader.close(); }
                return text.toString();
            }
        }
        throw new IllegalArgumentException("DOCX contains no document text");
    }
}
