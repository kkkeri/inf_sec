package org.example.lab1;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import javax.sql.DataSource;

@RestController
@RequestMapping("/api/data")
public class NotesController {
    private final JdbcTemplate jdbc;

    public NotesController(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public record NewNote(String text) {}
    public record Note(long id, String text) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@RequestBody NewNote note, @AuthenticationPrincipal Jwt jwt) {
        String text = note.text();
        if (text == null || text.isBlank() || text.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        jdbc.update("INSERT INTO notes (owner_name, body) VALUES (?, ?)",
                jwt.getSubject(), text);
    }

    @GetMapping
    public List<Note> list(@AuthenticationPrincipal Jwt jwt) {
        return jdbc.query(
                "SELECT id, body FROM notes WHERE owner_name = ? ORDER BY id",
                (row, index) -> new Note(
                        row.getLong("id"),
                        HtmlUtils.htmlEscape(row.getString("body"), "UTF-8")),
                jwt.getSubject());
    }
}