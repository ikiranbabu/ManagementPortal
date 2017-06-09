package org.radarcns.management.service;

import org.radarcns.management.domain.Device;
import org.radarcns.management.repository.DeviceRepository;
import org.radarcns.management.service.dto.DeviceDTO;
import org.radarcns.management.service.mapper.DeviceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing Device.
 */
@Service
@Transactional
public class DeviceService {

    private final Logger log = LoggerFactory.getLogger(DeviceService.class);
    
    private final DeviceRepository deviceRepository;

    private final DeviceMapper deviceMapper;

    public DeviceService(DeviceRepository deviceRepository, DeviceMapper deviceMapper) {
        this.deviceRepository = deviceRepository;
        this.deviceMapper = deviceMapper;
    }

    /**
     * Save a device.
     *
     * @param deviceDTO the entity to save
     * @return the persisted entity
     */
    public DeviceDTO save(DeviceDTO deviceDTO) {
        log.debug("Request to save Device : {}", deviceDTO);
        Device device = deviceMapper.deviceDTOToDevice(deviceDTO);
        device = deviceRepository.save(device);
        DeviceDTO result = deviceMapper.deviceToDeviceDTO(device);
        return result;
    }

    /**
     *  Get all the devices.
     *  
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<DeviceDTO> findAll() {
        log.debug("Request to get all Devices");
        List<DeviceDTO> result = deviceRepository.findAll().stream()
            .map(deviceMapper::deviceToDeviceDTO)
            .collect(Collectors.toCollection(LinkedList::new));

        return result;
    }

    /**
     *  Get one device by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public DeviceDTO findOne(Long id) {
        log.debug("Request to get Device : {}", id);
        Device device = deviceRepository.findOne(id);
        DeviceDTO deviceDTO = deviceMapper.deviceToDeviceDTO(device);
        return deviceDTO;
    }

    /**
     *  Delete the  device by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Device : {}", id);
        deviceRepository.delete(id);
    }
}