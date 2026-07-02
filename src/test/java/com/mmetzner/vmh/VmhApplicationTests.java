package com.mmetzner.vmh;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class VmhApplicationTests {

	@Test
	void applicationClassExists() {
		assertThat(VmhApplication.class).isNotNull();
	}

}
