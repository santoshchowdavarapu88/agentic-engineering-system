package com.santhosh.agentic_engineering_system.patch;

import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.model.ProposedFileChange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class PatchProposalMerger {
    public PatchProposal merge(PatchProposal implementation, PatchProposal tests) {
        List<ProposedFileChange> changes = new ArrayList<>(implementation.changes());
        changes.addAll(tests.changes());
        var assumptions = new LinkedHashSet<>(implementation.assumptions());
        assumptions.addAll(tests.assumptions());
        var risks = new LinkedHashSet<>(implementation.risks());
        risks.addAll(tests.risks());
        return new PatchProposal(implementation.summary() + "; " + tests.summary(),
                changes, List.copyOf(assumptions), List.copyOf(risks));
    }
}
