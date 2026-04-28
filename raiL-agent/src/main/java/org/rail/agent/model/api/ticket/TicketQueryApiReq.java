package org.rail.agent.model.api.ticket;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketQueryApiReq {
    private List<String> departureCodes;
    private List<String> arrivalCodes;
    private List<Integer> trainTypeIds;
    private List<Integer> seatTypes;
    private LocalDate departureDate;
}