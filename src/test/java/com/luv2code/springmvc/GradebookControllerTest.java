package com.luv2code.springmvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luv2code.springmvc.models.CollegeStudent;
import com.luv2code.springmvc.repository.HistoryGradesDao;
import com.luv2code.springmvc.repository.MathGradesDao;
import com.luv2code.springmvc.repository.ScienceGradesDao;
import com.luv2code.springmvc.repository.StudentDao;
import com.luv2code.springmvc.service.StudentAndGradeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

@TestPropertySource("/application-test.properties")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
public class GradebookControllerTest {

    private static MockHttpServletRequest request;

    @PersistenceContext
    private EntityManager entityManager;

    @Mock
    StudentAndGradeService studentAndGradeServiceMock;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StudentDao studentDao;

    @Autowired
    private MathGradesDao mathGradeDao;

    @Autowired
    private ScienceGradesDao scienceGradeDao;

    @Autowired
    private HistoryGradesDao historyGradeDao;

    @Autowired
    private StudentAndGradeService studentService;

    @Autowired
    private CollegeStudent collegeStudent;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;


    @Value("${sql.script.create.student}")
    private String sqlAddStudent;

    @Value("${sql.script.create.math.grade}")
    private String sqlAddMathGrade;

    @Value("${sql.script.create.science.grade}")
    private String sqlAddScienceGrade;

    @Value("${sql.script.create.history.grade}")
    private String sqlAddHistoryGrade;

    @Value("${sql.script.delete.student}")
    private String sqlDeleteStudent;

    @Value("${sql.script.delete.math.grade}")
    private String sqlDeleteMathGrade;

    @Value("${sql.script.delete.science.grade}")
    private String sqlDeleteScienceGrade;

    @Value("${sql.script.delete.history.grade}")
    private String sqlDeleteHistoryGrade;

    public static final MediaType APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON;

    @BeforeAll
    public static void setUp(){

        request = new MockHttpServletRequest();

        request.setParameter("firstname","Serhat");

        request.setParameter("lastname","Balkı");

        request.setParameter("emailAddress","serhatbalki97@gmail.com");
    }

    @BeforeEach
    public void setupDatabase() {
        jdbc.execute(sqlAddStudent);
        jdbc.execute(sqlAddMathGrade);
        jdbc.execute(sqlAddScienceGrade);
        jdbc.execute(sqlAddHistoryGrade);
    }

    @Test
    public void getStudentsHttpRequest() throws Exception{

        collegeStudent.setFirstname("Ferhat");
        collegeStudent.setLastname("Balkı");
        collegeStudent.setEmailAddress("ferhatbalki@gmail.com");
        entityManager.persist(collegeStudent);
        entityManager.flush();

        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$",hasSize(2)));
    }

    @Test
    public void createStudentHttpRequest() throws Exception {

        collegeStudent.setFirstname("Nare");
        collegeStudent.setLastname("Balkı");
        collegeStudent.setEmailAddress("narebalki@gmail.com");

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(collegeStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",hasSize(2)));

        CollegeStudent verifyStudent = studentDao.findByEmailAddress("narebalki@gmail.com");

        assertNotNull(verifyStudent,"student should be valid");
    }

    @Test
    public void deleteStudentHttpRequest() throws Exception {

        assertTrue(studentDao.findById(1).isPresent());

        mockMvc.perform(MockMvcRequestBuilders.delete("/student/{id}",1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$",hasSize(0)));

        assertFalse(studentDao.findById(1).isPresent());
    }

    @Test
    public void deleteStudentHttpRequestErrorPage() throws Exception{
        assertFalse(studentDao.findById(0).isPresent());

        mockMvc.perform(MockMvcRequestBuilders.delete("/student/{id}",0))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status",is(404)))
                .andExpect(jsonPath("$.message",is("Student or Grade was not found")));
    }

    @Test
    public void getStudentInformationHttpRequest() throws Exception {

        Optional<CollegeStudent> student = studentDao.findById(1);

        assertTrue(student.isPresent());

        mockMvc.perform(MockMvcRequestBuilders.get("/studentInformation/{id}",1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id",is(1)))
                .andExpect(jsonPath("$.firstname",is("Eric")))
                .andExpect(jsonPath("$.lastname",is("Roby")))
                .andExpect(jsonPath("$.emailAddress",is("eric.roby@luv2code_school.com")));
    }

    @Test
    public void getStudentInformationHttpRequestEmptyResponse() throws Exception{
        assertFalse(studentDao.findById(0).isPresent());

        mockMvc.perform(MockMvcRequestBuilders.get("/studentInformation/{id}",0))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.status",is(404)))
                .andExpect(jsonPath("$.message",is("Student or Grade was not found")));
    }

    @Test
    public void createAValidGradeHttpRequest() throws Exception{
        mockMvc.perform(post("/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .param("grade","85.00")
                .param("gradeType","math")
                .param("studentId","1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.firstname",is("Eric")))
                .andExpect(jsonPath("$.studentGrades.mathGradeResults",hasSize(2)));
    }

    @Test
    public void createAValidGradeHttpRequestStudentDoesNotExist() throws Exception{
        assertFalse(studentDao.findById(0).isPresent());

        mockMvc.perform(post("/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .param("grade","85.00")
                .param("gradeType","math")
                .param("studentId","0"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status",is(404)))
                .andExpect(jsonPath("$.message",is("Student or Grade was not found")));
    }

    @Test
    public void createANonValidGradeHttpRequestGradeTypeDoesNotExist() throws Exception{

        mockMvc.perform(post("/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .param("grade","85.00")
                .param("gradeType","literature")
                .param("studentId","1"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status",is(404)))
                .andExpect(jsonPath("$.message",is("Student or Grade was not found")));
    }

    @Test
    public void deleteGradeHttpRequest()throws Exception{
        assertTrue(studentDao.findById(1).isPresent());

        mockMvc.perform(MockMvcRequestBuilders.delete("/grades/{id}/{gradeType}",1,"math"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id",is(1)))
                .andExpect(jsonPath("$.firstname",is("Eric")))
                .andExpect(jsonPath("$.lastname",is("Roby")))
                .andExpect(jsonPath("$.studentGrades.mathGradeResults",hasSize(0)));
    }

    @Test
    public void deleteAValidGradeHttpRequestStudentDoesNotExist() throws Exception{

        mockMvc.perform(MockMvcRequestBuilders.delete("/grades/{id}/{gradeType}",2,"math"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status",is(404)))
                .andExpect(jsonPath("$.message",is("Student or Grade was not found")));
    }

    @Test
    public void deleteANonValidGradeHttpRequestGradeTypeDoesNotExist() throws Exception{

        mockMvc.perform(MockMvcRequestBuilders.delete("/grades/{id}/{gradeType}",1,"literature"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status",is(404)))
                .andExpect(jsonPath("$.message",is("Student or Grade was not found")));
    }


    @AfterEach
    public void setupAfterTransaction() {
        jdbc.execute(sqlDeleteStudent);
        jdbc.execute(sqlDeleteMathGrade);
        jdbc.execute(sqlDeleteScienceGrade);
        jdbc.execute(sqlDeleteHistoryGrade);
    }
}
