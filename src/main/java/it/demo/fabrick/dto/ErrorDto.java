package it.demo.fabrick.dto;

import java.util.List;

import lombok.Data;

@Data
public class ErrorDto {

	public String status;
	public List<Error> errors;

	@Data
	public class Error{
	    public String code;
	    public String description;
	    public String params;
	}

}
