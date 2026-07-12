package com.axel.alvarado.coworking_service.service;

import com.axel.alvarado.coworking_service.dto.SpaceRequest;
import com.axel.alvarado.coworking_service.dto.SpaceResponse;
import com.axel.alvarado.coworking_service.exception.ResourceNotFoundException;
import com.axel.alvarado.coworking_service.mapper.SpaceMapper;
import com.axel.alvarado.coworking_service.model.Space;
import com.axel.alvarado.coworking_service.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;

    @Transactional
    public SpaceResponse create(SpaceRequest request) {
        Space saved = spaceRepository.save(SpaceMapper.toEntity(request));
        return SpaceMapper.toResponse(saved);
    }

    @Transactional
    public SpaceResponse update(Long id, SpaceRequest request) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado: " + id));

        space.setName(request.name());
        space.setType(request.type());
        space.setCapacity(request.capacity());
        space.setLocation(request.location());
        space.setHourlyRate(request.hourlyRate());

        return SpaceMapper.toResponse(space);
    }

    @Transactional
    public void delete(Long id) {
        if (!spaceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Espacio no encontrado: " + id);
        }
        spaceRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public SpaceResponse getById(Long id) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio no encontrado: " + id));
        return SpaceMapper.toResponse(space);
    }

    @Transactional(readOnly = true)
    public List<SpaceResponse> getAll() {
        return spaceRepository.findAll().stream()
                .map(SpaceMapper::toResponse)
                .toList();
    }
}