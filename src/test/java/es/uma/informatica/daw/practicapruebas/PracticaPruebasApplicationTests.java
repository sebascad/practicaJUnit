package es.uma.informatica.daw.practicapruebas;

import es.uma.informatica.daw.practicapruebas.dtos.CitaDTO;
import es.uma.informatica.daw.practicapruebas.entidades.Cita;
import es.uma.informatica.daw.practicapruebas.entidades.EstadoCita;
import es.uma.informatica.daw.practicapruebas.repositorios.RepositorioCitas;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureTestRestTemplate
@DisplayName("En el servicio de citas")
class PracticaPruebasApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    int port;

    @Autowired
    private RepositorioCitas repositorioCitas;

    private String url(String rutaYConsulta) {
        return "http://localhost:" + port + rutaYConsulta;
    }

    @Nested
    @DisplayName("Tests para buscar las citas")
    public class TestBuscarCitas{
        @Test
        @DisplayName("al buscar por fecha se encuentran las citas de ese día")
        void buscarPorFecha() {
            // Guarda una cita para el 30 de abril a las 10:00am de 1 hora
            Cita cita = new Cita();
            cita.setCliente("Juan");
            cita.setInicio(LocalDateTime.parse("2026-04-30T10:00:00"));
            cita.setDuracion(60);
            repositorioCitas.save(cita);

            // Consulta las citas del 30 de abril
            ResponseEntity<CitaDTO[]> res = restTemplate.getForEntity(
                    url("/citas?fecha=2026-04-30"),
                    CitaDTO[].class
            );

            // Comprueba que hay una
            assertThat(res.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("al buscar por id de cita, la encuentra")
        void siLaCitaExisteDevuelveStatusCode200(){
            Cita cita = new Cita();
            cita.setCliente("Lloros Herrera");
            cita.setInicio(LocalDateTime.parse("2026-05-10T10:15:20"));
            cita.setDuracion(20);

            repositorioCitas.save(cita);
            ResponseEntity<CitaDTO> res = restTemplate.getForEntity(
                    url("/citas/" + cita.getId()), CitaDTO.class
            );

            Assertions.assertNotNull(res.getBody());
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
            //Comprobamos el Body
            assertThat(res.getBody().getId()).isEqualTo(cita.getId());
            assertThat(res.getBody().getCliente()).isEqualTo(cita.getCliente());
            assertThat(res.getBody().getFin()).isEqualTo(cita.getInicio().plusMinutes(cita.getDuracion()));
        }

        @Test
        @DisplayName("Al buscar una cita inexistente por id, devuelve 404")
        void siLaCitaNoExisteDevuelveStatusCode404(){
            ResponseEntity<CitaDTO> res = restTemplate.getForEntity(
                    url("/citas/200"), CitaDTO.class //Buscamos una cita que no existe
            );
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Test para confirmar citas")
    public class TestConfirmaCitas{
        @Test
        @DisplayName("Confirma una cita existente")
        void confirmaUnaCitaExistenteCREADA(){
            Cita cita = new Cita();
            cita.setCliente("Lloros Herrera");
            cita.setInicio(LocalDateTime.parse("2026-05-10T10:15:20"));
            cita.setDuracion(20);
            cita.setEstado(EstadoCita.CREADA);
            repositorioCitas.save(cita);

            ResponseEntity<CitaDTO> res = restTemplate.postForEntity(
                    url("/citas/" + cita.getId() + "/confirmar"),null, CitaDTO.class
            );

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
            //Comprobamos el Body
            assertThat(res.getBody().getId()).isEqualTo(cita.getId());
            assertThat(res.getBody().getCliente()).isEqualTo(cita.getCliente());
            assertThat(res.getBody().getFin()).isEqualTo(cita.getInicio().plusMinutes(cita.getDuracion()));
        }

        @Test
        @DisplayName("Cuando una cita ya esta confirmada, devuelve error 400")
        void devuelveStatusCode400CuandoUnaCitaNoSePuedeConfirmar(){
            Cita cita = new Cita();
            cita.setCliente("Lloros Herrera");
            cita.setInicio(LocalDateTime.parse("2026-05-10T10:15:20"));
            cita.setDuracion(20);
            cita.setEstado(EstadoCita.CONFIRMADA);
            repositorioCitas.save(cita);

            ResponseEntity<CitaDTO> res = restTemplate.postForEntity(
                    url("/citas/" + cita.getId() + "/confirmar"),null, CitaDTO.class
            );

            cita.setEstado(EstadoCita.CANCELADA);
            ResponseEntity<CitaDTO> res1 = restTemplate.postForEntity(
                    url("/citas/" + cita.getId() + "/confirmar"),null, CitaDTO.class
            );

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        }
        @Test
        @DisplayName("Cuando una cita no existe, devuelve error 404")
        void devuelve404CuandoUnaCitaNoExiste(){
            ResponseEntity<CitaDTO> res = restTemplate.postForEntity(
                    url("/citas/300/confirmar"),null, CitaDTO.class
            );
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Tests para cancelar citas")
    public class TestCancelaCitas{
        @Test
        @DisplayName("Cancela una cita existente correctamente")
        void cancelaUnaCitaYDevuelveElBody(){
            Cita cita = new Cita();
            cita.setCliente("Lloros Herrera");
            cita.setInicio(LocalDateTime.parse("2026-05-10T10:15:20"));
            cita.setDuracion(20);
            cita.setEstado(EstadoCita.CREADA);
            repositorioCitas.save(cita);

            ResponseEntity<CitaDTO> res = restTemplate.postForEntity(
                    url("/citas/" + cita.getId() + "/cancelar"),null, CitaDTO.class
            );

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
            //Comprobamos el Body
            assertThat(res.getBody().getId()).isEqualTo(cita.getId());
            assertThat(res.getBody().getCliente()).isEqualTo(cita.getCliente());
            assertThat(res.getBody().getFin()).isEqualTo(cita.getInicio().plusMinutes(cita.getDuracion()));
        }
        @Test
        @DisplayName("Cuando una cita no existe y se intenta cancelar, devuelve error 404")
        void devuelve404CuandoUnaCitaNoExiste(){
            ResponseEntity<CitaDTO> res = restTemplate.postForEntity(
                    url("/citas/300/cancelar"),null, CitaDTO.class
            );
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
        @Test
        @DisplayName("No se puede cancelar una cita si ya esta cancelada")
        void devuelveStatusCode400SiLaCitaYaEstaCancelada(){
            Cita cita = new Cita();
            cita.setCliente("Lloros Herrera");
            cita.setInicio(LocalDateTime.parse("2026-05-10T10:15:20"));
            cita.setDuracion(20);
            cita.setEstado(EstadoCita.CANCELADA);
            repositorioCitas.save(cita);

            ResponseEntity<CitaDTO> res = restTemplate.postForEntity(
                    url("/citas/" + cita.getId() + "/cancelar"),null, CitaDTO.class
            );

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        @Test
        @DisplayName("No se puede cancelar una cita si falta menos de un dia para la misma")
        void devuelveStatusCode400SiFaltanMenosDeUnDia(){
            Cita cita = new Cita();
            cita.setCliente("Lloros Herrera");
            cita.setInicio(LocalDateTime.now().plusMinutes(120)); //2 horas antes
            cita.setDuracion(20);
            cita.setEstado(EstadoCita.CREADA);
            repositorioCitas.save(cita);

            ResponseEntity<CitaDTO> res = restTemplate.postForEntity(
                    url("/citas/" + cita.getId() + "/cancelar"),null, CitaDTO.class
            );

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Test para probar inserción/creación")
    public class TestCrearCitas{
        @Test
        @DisplayName("Crea correctamente una cita")
        void creaCitaCorrectamenteYDevuelveStatusCode201(){
            Cita cita = new Cita();
            cita.setDuracion(60);
            cita.setInicio(LocalDateTime.parse("2026-05-01T10:00:00"));
            cita.setCliente("Juan");

            ResponseEntity<CitaDTO> res = restTemplate.postForEntity(
                    url("/citas"),cita, CitaDTO.class
            );

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(res.getHeaders().containsHeader("location"));
            //Comprobamos el Body
            Assertions.assertNotNull(res.getBody());
            assertThat(res.getBody().getCliente()).isEqualTo(cita.getCliente());
            assertThat(res.getBody().getFin()).isEqualTo(cita.getInicio().plusMinutes(cita.getDuracion()));
        }

        @Test
        @DisplayName("No detecta conflicto, si la cita esta CANCELADA")
        void noHaySolapamientoSiLaCitaEstaCancelada(){
            Cita cancelada = new Cita();
            cancelada.setCliente("Juan");
            cancelada.setInicio(LocalDateTime.parse("2026-05-01T10:00:00"));
            cancelada.setDuracion(60);
            cancelada.setEstado(EstadoCita.CANCELADA);
            repositorioCitas.save(cancelada);

            Cita nueva = new Cita();
            nueva.setCliente("Pepe");
            nueva.setInicio(LocalDateTime.parse("2026-05-01T10:20:00"));
            nueva.setDuracion(20);

            ResponseEntity<CitaDTO> res = restTemplate.postForEntity(
                    url("/citas"), nueva, CitaDTO.class
            );

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("Si se trata de insertar una cita,mientras que hay una en curso (Se solapan)")
        void devuelveStatusCode409SiSeSolapanDosCitas(){
            Cita cita = new Cita();
            cita.setDuracion(60);
            cita.setInicio(LocalDateTime.parse("2026-05-01T10:00:00"));
            cita.setCliente("Juan");
            repositorioCitas.save(cita); //Añadimos una cita

            Cita cita2 = new Cita();
            cita2.setDuracion(20);
            cita2.setInicio(LocalDateTime.parse("2026-05-01T10:00:00").plusMinutes(20)); //En mitad de la otra cita
            cita2.setCliente("Pepe");

            ResponseEntity<CitaDTO> res = restTemplate.postForEntity(
                    url("/citas"),cita2, CitaDTO.class
            );

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Si no cumple normas de duracion")
        void siNoCumpleNormasDeDuracionDevuelveStatusCode400(){
            Cita cita = new Cita();
            cita.setDuracion(121);
            cita.setInicio(LocalDateTime.parse("2026-05-01T10:00:00"));
            cita.setCliente("Pepe");

            ResponseEntity<CitaDTO> res1 = restTemplate.postForEntity(
                    url("/citas"),cita, CitaDTO.class
            );

            cita.setDuracion(14); //Probamos el rango completo tanto por arriba como por abajo

            ResponseEntity<CitaDTO> res2 = restTemplate.postForEntity(
                    url("/citas"),cita, CitaDTO.class
            );

            assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("No cumple Horario Laboral")
        void siNoCumpleHorarioLaboralDevuelveStatusCode400(){
            Cita cita = new Cita();
            cita.setDuracion(60);
            cita.setInicio(LocalDateTime.parse("2026-05-01T08:59:00"));
            cita.setCliente("Pepe");

            ResponseEntity<CitaDTO> res1 = restTemplate.postForEntity(
                    url("/citas"),cita, CitaDTO.class
            );

            cita.setInicio(LocalDateTime.parse("2026-05-01T18:59:00"));

            ResponseEntity<CitaDTO> res2 = restTemplate.postForEntity(
                    url("/citas"),cita, CitaDTO.class
            );

            cita.setInicio(LocalDateTime.parse("2026-05-01T17:59:00")); //No debe permitir una cita un minuto antes de que cierre
            ResponseEntity<CitaDTO> res3 = restTemplate.postForEntity(
                    url("/citas"),cita, CitaDTO.class
            );
            assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(res3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
