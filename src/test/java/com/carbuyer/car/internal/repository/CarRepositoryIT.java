package com.carbuyer.car.internal.repository;

import com.carbuyer.DataJpaIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CarRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private CarRepository carRepository;
}
