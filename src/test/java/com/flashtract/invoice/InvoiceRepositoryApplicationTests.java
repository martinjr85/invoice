package com.flashtract.invoice;

import com.flashtract.invoice.model.*;
import com.flashtract.invoice.repository.ContractRepository;
import com.flashtract.invoice.repository.InvoiceRepository;
import com.flashtract.invoice.repository.UserRepository;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.config.HypermediaRestTemplateConfigurer;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

import static com.flashtract.invoice.model.Status.Completed;
import static com.flashtract.invoice.model.Status.InProgress;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InvoiceRepositoryApplicationTests {

	private final static Faker faker = new Faker();
	private static final String CONTRACTS_URI = "/contracts";
	private static final String INVOICES_URI = "/invoices";
	private static final String INVOICES_VOID_URI = "/invoices/{invoiceId}/void";

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContractRepository contractRepository;
	@Autowired
	private InvoiceRepository invoiceRepository;
	@Autowired
	private TestRestTemplate restTemplate;

	@LocalServerPort
	private int port;

	private User clientUser = User.builder().userType(UserType.Client).name(faker.funnyName().name()).build();
	private User vendorUser = User.builder().userType(UserType.Vendor).name(faker.funnyName().name()).build();
	private String endpoint;

	@Bean
	RestTemplateCustomizer hypermediaRestTemplateCustomizer(HypermediaRestTemplateConfigurer configurer) {
		return restTemplate -> {
			configurer.registerHypermediaTypes(restTemplate);
		};
	}

	@BeforeEach
	void beforeEach() {
		clientUser = userRepository.save(clientUser);
		vendorUser = userRepository.save(vendorUser);
		endpoint = "http://localhost:" + port;
	}

	@AfterEach
	void afterEach() {
		invoiceRepository.deleteAll();
		contractRepository.deleteAll();
		userRepository.deleteAll();;
	}

	@Test
	void createContractWithVendorUser() {
		Contract contract = Contract.builder()
				.amount(100.0)
				.description(faker.hitchhikersGuideToTheGalaxy().quote())
				.userId(vendorUser.getUserId())
				.build();
		ResponseEntity<EntityModel<Contract>> contractResponse = createContract(contract);
		assertTrue(contractResponse.getBody() instanceof EntityModel);
		Contract returnedContract = contractResponse.getBody().getContent();

		assertEquals(Status.Approved, returnedContract.getStatus());
		assertNotNull(returnedContract.getCreatedAt());
	}

	@Test
	void createContractWithUnknownUser() {
		Contract contract = Contract.builder()
				.amount(100.0)
				.description(faker.hitchhikersGuideToTheGalaxy().quote())
				.userId(UUID.randomUUID())
				.build();
		ResponseEntity<InvoiceError> response = createEntityWithExpectedError(contract, CONTRACTS_URI);
		assertTrue(response.getBody() instanceof InvoiceError);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	void createContractWithClientUser() {
		Contract contract = Contract.builder()
				.amount(100.0)
				.description(faker.hitchhikersGuideToTheGalaxy().quote())
				.userId(clientUser.getUserId())
				.build();
		ResponseEntity<InvoiceError> response = createEntityWithExpectedError(contract, CONTRACTS_URI);
		assertTrue(response.getBody() instanceof InvoiceError);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	void createContractWithNullUser() {
		Contract contract = Contract.builder()
				.amount(100.0)
				.description(faker.hitchhikersGuideToTheGalaxy().quote())
				.build();
		ResponseEntity<InvoiceError> response = createEntityWithExpectedError(contract, CONTRACTS_URI);
		assertTrue(response.getBody() instanceof InvoiceError);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	void createContractWithNegativeAmount() {
		Contract contract = Contract.builder()
				.amount(-100.0)
				.description(faker.hitchhikersGuideToTheGalaxy().quote())
				.userId(vendorUser.getUserId())
				.build();
		ResponseEntity<InvoiceError> response = createEntityWithExpectedError(contract, CONTRACTS_URI);
		assertTrue(response.getBody() instanceof InvoiceError);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	void createInvoice() {
		Contract contract = contractRepository.save(Contract.builder()
				.userId(vendorUser.getUserId())
				.amount(100.0).build());
		Invoice invoice = Invoice.builder()
				.value(49.0)
				.contractId(contract.getContractId())
				.build();
		ResponseEntity<EntityModel<Invoice>> response = createInvoice(invoice);
		assertTrue(response.getBody() instanceof EntityModel);
		Invoice returnedInvoice = response.getBody().getContent();

		assertEquals(Status.Approved, returnedInvoice.getStatus());
		assertNotNull(returnedInvoice.getCreatedAt());

		// refresh contract
		contract = contractRepository.findById(contract.getContractId()).get();
		assertEquals(InProgress, contract.getStatus());

		//check remaining
		double contractTotal = invoiceRepository.sumValueByContractId(contract.getContractId());
		assertEquals(51.0, contract.getAmount() - contractTotal);
	}

	@Test
	void createTwoValidInvoices() {
		Contract contract = contractRepository.save(Contract.builder()
				.userId(vendorUser.getUserId())
				.amount(100.0).build());
		Invoice invoice = Invoice.builder()
				.value(49.0)
				.contractId(contract.getContractId())
				.build();
		createInvoice(invoice);
		Invoice invoice2 = Invoice.builder()
				.value(49.0)
				.contractId(contract.getContractId())
				.build();
		ResponseEntity<EntityModel<Invoice>> response = createInvoice(invoice2);
		assertTrue(response.getBody() instanceof EntityModel);
		Invoice returnedInvoice = response.getBody().getContent();

		assertEquals(Status.Approved, returnedInvoice.getStatus());
		assertNotNull(returnedInvoice.getCreatedAt());

		// refresh contract
		contract = contractRepository.findById(contract.getContractId()).get();
		assertEquals(InProgress, contract.getStatus());

		//check remaining
		double contractTotal = invoiceRepository.sumValueByContractId(contract.getContractId());
		assertEquals(2.0, contract.getAmount() - contractTotal);
	}

	@Test
	void createInvoiceForCompletedContract() {
		Contract contract = contractRepository.save(Contract.builder()
				.userId(vendorUser.getUserId())
				.amount(100.0).build());
		Invoice invoice = Invoice.builder()
				.value(100.0)
				.contractId(contract.getContractId())
				.build();

		createInvoice(invoice);
		Invoice invoice2 = Invoice.builder()
				.value(100.0)
				.contractId(contract.getContractId())
				.build();

		ResponseEntity<InvoiceError> response = createEntityWithExpectedError(invoice2, INVOICES_URI);
		assertTrue(response.getBody() instanceof InvoiceError);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	void createThreeInvoicesWithOneVoidedInvoice() {
		Contract contract = contractRepository.save(Contract.builder()
				.userId(vendorUser.getUserId())
				.amount(100.0).build());
		Invoice invoice = Invoice.builder()
				.value(49.0)
				.contractId(contract.getContractId())
				.build();
		createInvoice(invoice);
		Invoice invoice2 = Invoice.builder()
				.value(51.0)
				.contractId(contract.getContractId())
				.build();
		Invoice returnedInvoice = createInvoice(invoice2).getBody().getContent();
		voidInvoice(returnedInvoice.getInvoiceId());

		ResponseEntity<EntityModel<Invoice>> response = createInvoice(invoice2);
		assertTrue(response.getBody() instanceof EntityModel);
		returnedInvoice = response.getBody().getContent();

		assertEquals(Status.Approved, returnedInvoice.getStatus());
		assertNotNull(returnedInvoice.getCreatedAt());

		// refresh contract
		contract = contractRepository.findById(contract.getContractId()).get();
		assertEquals(Completed, contract.getStatus());

		//check remaining
		double contractTotal = invoiceRepository.sumValueByContractId(contract.getContractId());
		assertEquals(0, contract.getAmount() - contractTotal);

		//3 invoices total, 1 is void
		assertEquals(3, invoiceRepository.count());
	}

	@Test
	void createInvoiceWithNegativeAmount() {
		Contract contract = contractRepository.save(Contract.builder()
				.userId(vendorUser.getUserId())
				.amount(100.0).build());
		Invoice invoice = Invoice.builder()
				.value(-100.0)
				.contractId(contract.getContractId())
				.build();
		ResponseEntity<InvoiceError> response = createEntityWithExpectedError(invoice, INVOICES_URI);
		assertTrue(response.getBody() instanceof InvoiceError);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	void createInvoiceWithUnknownContract() {
		Invoice invoice = Invoice.builder()
				.value(100.0)
				.contractId(UUID.randomUUID())
				.build();
		ResponseEntity<InvoiceError> response = createEntityWithExpectedError(invoice, INVOICES_URI);
		assertTrue(response.getBody() instanceof InvoiceError);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	void voidInvoiceWithUnknownInvoiceId() {
		ResponseEntity<EntityModel<Invoice>> response = voidInvoice(UUID.randomUUID());
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	private ResponseEntity<EntityModel<Contract>> createContract(Contract contract) {
		return restTemplate.exchange(
				endpoint + CONTRACTS_URI,
				HttpMethod.POST,
				new HttpEntity<>(contract),
				new TypeReferences.EntityModelType<>() {
				}
		);
	}

	private ResponseEntity<EntityModel<Invoice>> createInvoice(Invoice invoice) {
		return restTemplate.exchange(
				endpoint + INVOICES_URI,
				HttpMethod.POST,
				new HttpEntity<>(invoice),
				new TypeReferences.EntityModelType<>() {
				}
		);
	}

	private ResponseEntity<EntityModel<Invoice>> voidInvoice(UUID invoiceId) {
		return restTemplate.exchange(
				UriComponentsBuilder.fromHttpUrl(endpoint + INVOICES_VOID_URI).build(invoiceId),
				HttpMethod.PUT,
				null,
				new TypeReferences.EntityModelType<>() {
				}
		);
	}

	private ResponseEntity<InvoiceError> createEntityWithExpectedError(Object entity, String uri) {
		return restTemplate.exchange(
				endpoint + uri,
				HttpMethod.POST,
				new HttpEntity<>(entity),
				InvoiceError.class
		);
	}
}
