package team.bham.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import team.bham.domain.TeamProfile;
import team.bham.domain.UserProfile;
import team.bham.repository.TeamProfileRepository;
import team.bham.security.SecurityUtils;
import team.bham.service.TeamProfileService;
import team.bham.service.dto.TeamProfileDTO;
import team.bham.web.rest.errors.BadRequestAlertException;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link team.bham.domain.TeamProfile}.
 */
@RestController
@RequestMapping("/api/team-profiles")
public class TeamProfileResource {

    private static final Logger LOG = LoggerFactory.getLogger(TeamProfileResource.class);

    private static final String ENTITY_NAME = "teamProfile";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final TeamProfileService teamProfileService;
    private final TeamProfileRepository teamProfileRepository;

    public TeamProfileResource(TeamProfileService teamProfileService, TeamProfileRepository teamProfileRepository) {
        this.teamProfileService = teamProfileService;
        this.teamProfileRepository = teamProfileRepository;
    }

    /**
     * {@code POST  /team-profiles} : Create a new teamProfile.
     *
     * @param teamProfileDTO the teamProfileDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new teamProfileDTO, or with status {@code 400 (Bad Request)} if the teamProfile has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<TeamProfileDTO> createTeamProfile(@Valid @RequestBody TeamProfileDTO teamProfileDTO) throws URISyntaxException {
        LOG.debug("REST request to save TeamProfile : {}", teamProfileDTO);
        if (teamProfileDTO.getId() != null) {
            throw new BadRequestAlertException("A new teamProfile cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new BadRequestAlertException("Only admins can create " + ENTITY_NAME, ENTITY_NAME, "idinvalid");
        }

        teamProfileDTO = teamProfileService.save(teamProfileDTO);
        return ResponseEntity.created(new URI("/api/team-profiles/" + teamProfileDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, teamProfileDTO.getId().toString()))
            .body(teamProfileDTO);
    }

    /**
     * {@code PUT  /team-profiles/:id} : Updates an existing teamProfile.
     *
     * @param id             the id of the teamProfileDTO to save.
     * @param teamProfileDTO the teamProfileDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated teamProfileDTO.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TeamProfileDTO> updateTeamProfile(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody TeamProfileDTO teamProfileDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update TeamProfile : {}, {}", id, teamProfileDTO);
        if (teamProfileDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, teamProfileDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!teamProfileRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Checking if user is a member of the team
        String currentUserLogin = SecurityUtils.getCurrentUserLogin().orElse(null);
        TeamProfile teamProfileE = teamProfileRepository.findById(teamProfileDTO.getId()).orElse(null);
        boolean isMember = false;

        if (!isAdmin && teamProfileE != null && currentUserLogin != null) {
            Set<UserProfile> teamMembers = teamProfileE.getTeamMembers();
            isMember = teamMembers.stream().anyMatch(member -> member.getUser().getLogin().equals(currentUserLogin));
        }

        if (!isAdmin && !isMember) {
            throw new BadRequestAlertException("Only admins or team members can edit the team profile", ENTITY_NAME, "idinvalid");
        }

        teamProfileDTO = teamProfileService.update(teamProfileDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, teamProfileDTO.getId().toString()))
            .body(teamProfileDTO);
    }

    /**
     * {@code PATCH  /team-profiles/:id} : Partial updates given fields of an existing teamProfile.
     *
     * @param id             the id of the teamProfileDTO to save.
     * @param teamProfileDTO the teamProfileDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated teamProfileDTO.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<TeamProfileDTO> partialUpdateTeamProfile(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody TeamProfileDTO teamProfileDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update TeamProfile partially : {}, {}", id, teamProfileDTO);
        if (teamProfileDTO.getId() == null || !Objects.equals(id, teamProfileDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!teamProfileRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Checking if user is a member of the team
        String currentUserLogin = SecurityUtils.getCurrentUserLogin().orElse(null);
        TeamProfile teamProfileE = teamProfileRepository.findById(teamProfileDTO.getId()).orElse(null);
        boolean isMember = false;

        if (!isAdmin && teamProfileE != null && currentUserLogin != null) {
            Set<UserProfile> teamMembers = teamProfileE.getTeamMembers();
            isMember = teamMembers.stream().anyMatch(member -> member.getUser().getLogin().equals(currentUserLogin));
        }

        if (!isAdmin && !isMember) {
            throw new BadRequestAlertException("Only admins or team members can edit the team profile", ENTITY_NAME, "idinvalid");
        }

        Optional<TeamProfileDTO> result = teamProfileService.partialUpdate(teamProfileDTO);
        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, teamProfileDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /team-profiles} : get all the teamProfiles.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of teamProfiles in body.
     */
    @GetMapping("")
    public List<TeamProfileDTO> getAllTeamProfiles() {
        LOG.debug("REST request to get all TeamProfiles");
        return teamProfileService.findAll();
    }

    /**
     * {@code GET  /team-profiles/:id} : get the "id" teamProfile.
     *
     * @param id the id of the teamProfileDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the teamProfileDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeamProfileDTO> getTeamProfile(@PathVariable("id") Long id) {
        LOG.debug("REST request to get TeamProfile : {}", id);
        Optional<TeamProfileDTO> teamProfileDTO = teamProfileService.findOne(id);
        return ResponseUtil.wrapOrNotFound(teamProfileDTO);
    }

    /**
     * {@code DELETE  /team-profiles/:id} : delete the "id" teamProfile.
     *
     * @param id the id of the teamProfileDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeamProfile(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete TeamProfile : {}", id);

        // Checking if user is an admin
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new BadRequestAlertException("Only admins can delete team profiles", ENTITY_NAME, "idinvalid");
        }

        teamProfileService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
