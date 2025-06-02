package ir.hamqadam.core.controller.dto.common;

import lombok.Data;
import org.springframework.data.domain.Page;
import java.util.List;

@Data
public class PageableResponseDTO<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;
    private int numberOfElements;
    private boolean empty;

    public PageableResponseDTO(Page<T> page, List<T> content) {
        this.content = content; // Use transformed content (e.g., list of DTOs)
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.last = page.isLast();
        this.first = page.isFirst();
        this.numberOfElements = page.getNumberOfElements();
        this.empty = page.isEmpty();
    }
    public PageableResponseDTO(List<T> content, int pageNumber, int pageSize, long totalElements, int totalPages, boolean last, boolean first, int numberOfElements, boolean empty) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
        this.first = first;
        this.numberOfElements = numberOfElements;
        this.empty = empty;
    }
}