package com.etiya.rentACar.business.concretes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.etiya.rentACar.business.abstracts.*;
import com.etiya.rentACar.business.constants.messages.Messages;
import com.etiya.rentACar.business.dtos.CarDamageSearchListDto;
import com.etiya.rentACar.core.utilities.business.BusinessRules;
import com.etiya.rentACar.core.utilities.results.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.etiya.rentACar.business.dtos.CarDetailDto;
import com.etiya.rentACar.business.dtos.CarSearchListDto;
import com.etiya.rentACar.business.request.carRequests.CreateCarRequest;
import com.etiya.rentACar.business.request.carRequests.DeleteCarRequest;
import com.etiya.rentACar.business.request.carRequests.UpdateCarRequest;
import com.etiya.rentACar.core.utilities.mapping.ModelMapperService;
import com.etiya.rentACar.dataAccess.abstracts.CarDao;
import com.etiya.rentACar.entities.Car;
import com.etiya.rentACar.entities.CarImage;
import com.etiya.rentACar.entities.complexTypes.CarDetail;

@Service
public class CarManager implements CarService {

	private CarDao carDao;
	private ModelMapperService modelMapperService;
	private CarImageService carImageService;
	private MaintenanceService maintenanceService;
	private CarDamageService carDamageService;
	private CityService cityService;
	private MessageService messageService;
	private Environment environment;

	@Autowired
	public CarManager(CarDao carDao, ModelMapperService modelMapperService, @Lazy CarImageService carImageService,
					  @Lazy MaintenanceService maintenanceService, @Lazy CarDamageService carDamageService,CityService cityService,
					  MessageService messageService, Environment environment) {
		super();
		this.carDao = carDao;
		this.modelMapperService = modelMapperService;
		this.carImageService = carImageService;
		this.maintenanceService = maintenanceService;
		this.carDamageService = carDamageService;
		this.cityService = cityService;
		this.messageService = messageService;
		this.environment = environment;
	}

	@Override
	public List<CarSearchListDto> getCars() {

		List<Car> list = this.carDao.findAll();
		List<CarSearchListDto> response = list.stream().map(car -> modelMapperService.forDto()
				.map(car, CarSearchListDto.class)).collect(Collectors.toList());

		return response;
	}

	@Override
	public Result save(CreateCarRequest createCarRequest) {
		Result result = BusinessRules.run(checkIfModelYearIsNumeric(createCarRequest.getModelYear()));
		if (result != null){
			return result;
		}
		Car car = modelMapperService.forRequest().map(createCarRequest, Car.class);
		this.carDao.save(car);
		return new SuccessResult(messageService.getMessage(Messages.addCar));
	}

	@Override
	public Result delete(DeleteCarRequest deleteCarRequest) {
		Result result = BusinessRules.run(checkExistingCar(deleteCarRequest.getCarId()));
		if (result != null){
			return result;
		}
		Car car = modelMapperService.forRequest().map(deleteCarRequest, Car.class);
		this.carDao.delete(car);
		return new SuccessResult(messageService.getMessage(Messages.deleteCar));
	}

	@Override
	public Result update(UpdateCarRequest updateCarRequest) {
		Result result = BusinessRules.run(checkExistingCar(updateCarRequest.getCarId()), checkIfModelYearIsNumeric(updateCarRequest.getModelYear()));
		if (result != null) {
			return result;
		}

		Car car = modelMapperService.forRequest().map(updateCarRequest, Car.class);
		this.carDao.save(car);
		return new SuccessResult(messageService.getMessage(Messages.updateCar));
	}

	@Override
	public DataResult<List<CarDetail>> getCarWithColorAndBrandDetails() {
		List<CarDetail> result = this.carDao.getCarWithColorAndBrandDetails();
		List<CarDetail> response = deleteCarsOnMaintenanceFromCarDetailList(result);
		return new SuccessDataResult<List<CarDetail>>(response);
	}

	@Override
	public DataResult<List<CarSearchListDto>> getByBrandId(int brandId) {
		List<Car> cars = this.carDao.getByBrand_BrandId(brandId);
		if (cars.isEmpty()){
			return new ErrorDataResult<List<CarSearchListDto>>(null,
					messageService.getMessage(Messages.carListIsEmpty));
		}
		List<CarSearchListDto> response = cars.stream().map(car -> modelMapperService.forDto()
				.map(car, CarSearchListDto.class)).collect(Collectors.toList());
		List<CarSearchListDto> finalResponse = deleteCarsOnMaintenanceFromCarSearchListDtoList(response);
		return new SuccessDataResult<List<CarSearchListDto>>(finalResponse);
	}

	@Override
	public DataResult<List<CarSearchListDto>> getByColorId(int colorId) {
		List<Car> cars = this.carDao.getByColor_ColorId(colorId);
		if (cars.isEmpty()){
			return new ErrorDataResult<List<CarSearchListDto>>(null,
					messageService.getMessage(Messages.carListIsEmpty));
		}
		List<CarSearchListDto> response = cars.stream().map(car -> modelMapperService.forDto()
				.map(car, CarSearchListDto.class)).collect(Collectors.toList());
		List<CarSearchListDto> finalResponse = deleteCarsOnMaintenanceFromCarSearchListDtoList(response);
		return new SuccessDataResult<List<CarSearchListDto>>(finalResponse);
	}

	@Override
	public DataResult<List<CarSearchListDto>> getByCity(int cityId) {
		List<Car> list = carDao.getByCity(cityService.getByCityId(cityId));
		if (list.isEmpty()){
			return new ErrorDataResult<List<CarSearchListDto>>(null,
					messageService.getMessage(Messages.carListIsEmpty));
		}
		List<CarSearchListDto> result = list.stream().map(car -> modelMapperService.forDto().
				map(car, CarSearchListDto.class)).collect(Collectors.toList());
		List<CarSearchListDto> response = deleteCarsOnMaintenanceFromCarSearchListDtoList(result);
		return new SuccessDataResult<List<CarSearchListDto>>(response);
	}

	@Override
	public DataResult<CarDetailDto> getCarDetailsByCarId(int carId) {
		Result result = BusinessRules.run(checkExistingCar(carId));
		if (result != null) {
			return new ErrorDataResult<CarDetailDto>(null,
					messageService.getMessage(Messages.carNotFound));
		}
		Car car = this.carDao.getById(carId);
		CarDetailDto carDetailDto = modelMapperService.forDto().map(car, CarDetailDto.class);
		carDetailDto.setImagePaths(getCarImagePathsAsList(carId));
		carDetailDto.setDamageDescriptions(getCarDamageDescriptionsAsList(carId));
		return new SuccessDataResult<CarDetailDto>(carDetailDto);
	}

	@Override
	public Car getCarAsElementByCarId(int carId) {
		Car car = carDao.getById(carId);
		return car;
	}

	@Override
	public void updateCarCity(int carId, int cityId) {
		Car car = carDao.getById(carId);
		car.setCity(cityService.getByCityId(cityId));
	}

	@Override
	public void updateCarKilometer(int carId, int kilometer) {
		Car car = carDao.getById(carId);
		car.setKilometer(kilometer);
	}

	public Result checkExistingCar(int carId) {
		boolean isExist = carDao.existsById(carId);
		if (!isExist) {
			return new ErrorResult(messageService.getMessage(Messages.carNotFound));
		}
		return new SuccessResult();
	}

	@Override
	public DataResult<List<CarDetail>> getCarsThatAreNotOnMaintenance() {
		List<Car> list = this.carDao.findAll();
		List<CarDetail> result = list.stream().map(car -> modelMapperService.forDto().map(car, CarDetail.class)).collect(Collectors.toList());
		List<CarDetail> response = deleteCarsOnMaintenanceFromCarDetailList(result);
		return new SuccessDataResult<List<CarDetail>>(response);
	}

	private List<CarDetail> deleteCarsOnMaintenanceFromCarDetailList(List<CarDetail> list) {
		List<CarDetail> response = new ArrayList<CarDetail>();
		for (CarDetail cars : list) {
			response.add(cars);
		}
		for (CarDetail carDetail : list) {
			int carId = carDetail.getCarId();
			boolean isCarOnMaintenance = maintenanceService.checkIfCarIsOnMaintenance(carId).isSuccess();
			if (!isCarOnMaintenance) {
				response.remove(carDetail);
			}
		}
		return response;
	}

	private List<CarSearchListDto> deleteCarsOnMaintenanceFromCarSearchListDtoList(List<CarSearchListDto> list) {
		List<CarSearchListDto> response = new ArrayList<CarSearchListDto>();
		for (CarSearchListDto cars : list) {
			response.add(cars);
		}
		for (CarSearchListDto carSearchListDto : list) {
			int carId = carSearchListDto.getCarId();
			boolean isCarOnMaintenance = maintenanceService.checkIfCarIsOnMaintenance(carId).isSuccess();
			if (!isCarOnMaintenance) {
				response.remove(carSearchListDto);
			}
		}
		return response;
	}

	public List<String> getCarImagePathsAsList(int carId) {
		List<String> list = new ArrayList<String>();
		List<CarImage> carImages = carImageService.getCarImageListByCarId(carId);
		for (CarImage image : carImages) {
			list.add(image.getImagePath());
		}
		if (list.size() == 0) {
			list.add(environment.getProperty("default.image.path"));
		}
		return list;
	}

	private List<String> getCarDamageDescriptionsAsList(int carId){
		List<String> list = new ArrayList<String>();
		List<CarDamageSearchListDto> carDamageSearchListDtos = carDamageService.getDamagesByCarId(carId).getData();
		for (CarDamageSearchListDto carDamageSearchListDto : carDamageSearchListDtos) {
			list.add(carDamageSearchListDto.getDamageDescription());
		}
		if (list.size() == 0){
			list.add(messageService.getMessage(Messages.carDamageNotFound));
		}
		return list;
	}

	public Result checkIfModelYearIsNumeric(String modelYear){
		if(StringUtils.isNumeric(modelYear)){
			return new SuccessResult();
		}
		else {
			return new ErrorResult(messageService.getMessage(Messages.invalidModelYearFormat));
		}
	}

}